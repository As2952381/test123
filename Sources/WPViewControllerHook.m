//
//  WPViewControllerHook.m — UIViewController Method Swizzling 实现
//
//  策略：
//  - Swizzle UIViewController.viewDidAppear: → 遍历当前 VC 的 view 树
//  - Swizzle UIViewController.viewWillDisappear: → 清理状态（可选）
//
#import "WPViewControllerHook.h"
#import "WPRedPacketDetector.h"
#import "WPAutoClicker.h"
#import "WPNetworkChecker.h"
#import "WPCommon.h"
#import "WPConfig.h"
#import <objc/runtime.h>

static WPRedPacketDetector *g_detector = nil;
static WPAutoClicker *g_clicker = nil;

@implementation WPViewControllerHook

+ (void)install {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        g_clicker = [[WPAutoClicker alloc] init];
        g_detector = [[WPRedPacketDetector alloc] initWithClicker:g_clicker];

        [self swizzleMethod:@selector(viewDidAppear:)
                 withMethod:@selector(wp_viewDidAppear:)];

        WPLog(@"UIViewController Hook 安装完成");
    });
}

+ (void)swizzleMethod:(SEL)originalSel withMethod:(SEL)swizzledSel {
    Class class = [UIViewController class];
    Method original = class_getInstanceMethod(class, originalSel);
    Method swizzled = class_getInstanceMethod(class, swizzledSel);

    BOOL didAdd = class_addMethod(class, originalSel,
                                  method_getImplementation(swizzled),
                                  method_getTypeEncoding(swizzled));
    if (didAdd) {
        class_replaceMethod(class, swizzledSel,
                            method_getImplementation(original),
                            method_getTypeEncoding(original));
    } else {
        method_exchangeImplementations(original, swizzled);
    }
}

// ═══════════════════════════════════════════
// Swizzled viewDidAppear:
// ═══════════════════════════════════════════
- (void)wp_viewDidAppear:(BOOL)animated {
    // 先调用原始实现
    [self wp_viewDidAppear:animated];

    @try {
        // 检查是否启用
        if (![WPConfig shared].enabled) return;

        // 网络条件
        if (![WPNetworkChecker shouldProceed]) return;

        NSString *vcName = NSStringFromClass([self class]);

        // 跳过系统 VC（减少功耗）
        if ([vcName hasPrefix:@"UI"] || [vcName hasPrefix:@"_UI"]) return;
        if ([self isKindOfClass:NSClassFromString(@"UIInputWindowController")]) return;

        // 延时 300ms 等待渲染完成
        __weak UIViewController *weakSelf = self;
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 300 * NSEC_PER_MSEC),
                       dispatch_get_main_queue(), ^{
            __strong UIViewController *strongSelf = weakSelf;
            if (!strongSelf || !strongSelf.isViewLoaded || !strongSelf.view.window) return;

            NSInteger found = [g_detector scanView:strongSelf.view vcName:vcName];
            if (found > 0) {
                WPLog(@"检测完成: %@ | 发现 %ld 个目标", vcName, (long)found);
            }
        });
    } @catch (NSException *e) {
        WPLog(@"Hook 异常: %@", e);
    }
}

@end
