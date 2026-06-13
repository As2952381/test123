//
//  WPAutoClicker.m — 自动点击实现
//
#import "WPAutoClicker.h"
#import "WPCommon.h"
#import "WPConfig.h"

/// 模拟触摸事件（对非 UIControl 的 view 使用）
static void simulateTouch(UIView *view) {
    if (!view) return;

    // 方案 A: 对 UIControl 发送事件
    if ([view isKindOfClass:[UIControl class]]) {
        UIControl *control = (UIControl *)view;
        [control sendActionsForControlEvents:UIControlEventTouchUpInside];
        return;
    }

    // 方案 B: 查找父视图链中的第一个 UIControl
    UIView *sv = view.superview;
    while (sv) {
        if ([sv isKindOfClass:[UIControl class]]) {
            [(UIControl *)sv sendActionsForControlEvents:UIControlEventTouchUpInside];
            return;
        }
        sv = sv.superview;
    }

    // 方案 C: 对 view 直接发送触摸事件
    // 找到 view 的中心位置
    CGPoint center = CGPointMake(CGRectGetMidX(view.bounds),
                                 CGRectGetMidY(view.bounds));
    UIView *target = [view hitTest:center withEvent:nil] ?: view;
    if ([target isKindOfClass:[UIControl class]]) {
        [(UIControl *)target sendActionsForControlEvents:UIControlEventTouchUpInside];
    }
}

@implementation WPAutoClicker {
    NSMutableSet<NSString *> *_processedIds;
    CFAbsoluteTime _lastClickTime;
}

- (instancetype)init {
    self = [super init];
    if (self) {
        _processedIds = [NSMutableSet set];
        _lastClickTime = 0;
    }
    return self;
}

- (BOOL)performClick:(UIView *)view
            uniqueId:(NSString *)uniqueId
                desc:(NSString *)desc {
    if (!view || !uniqueId) return NO;

    // 1. 防重复
    if ([_processedIds containsObject:uniqueId]) {
        WPLog(@"跳过已处理: %@ | ID: %@", desc, uniqueId);
        return NO;
    }

    // 2. 检查点击间隔（最小 500ms）
    CFAbsoluteTime now = CFAbsoluteTimeGetCurrent();
    if (now - _lastClickTime < 0.5) {
        WPLog(@"点击过于频繁，跳过: %@", desc);
        return NO;
    }

    // 3. 标记已处理
    [_processedIds addObject:uniqueId];
    _lastClickTime = now;

    // 4. 延时 + 点击
    NSTimeInterval delay = [[WPConfig shared] randomDelay];
    WPLog(@"发现目标: %@ | 延时: %.2fs", desc, delay);

    __weak typeof(view) weakView = view;
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW,
                                 (int64_t)(delay * NSEC_PER_SEC)),
                   dispatch_get_main_queue(), ^{
        __strong typeof(weakView) strongView = weakView;
        if (strongView && strongView.window) {
            simulateTouch(strongView);
            WPLog(@"点击执行: %@", desc);
        }
    });

    return YES;
}

- (void)performClickImmediately:(UIView *)view
                       uniqueId:(NSString *)uniqueId
                           desc:(NSString *)desc {
    if (!view || !uniqueId) return;
    if ([_processedIds containsObject:uniqueId]) return;
    [_processedIds addObject:uniqueId];
    if (view.window) {
        simulateTouch(view);
    }
}

+ (NSString *)makeUniqueId:(NSString *)vcClass
                      text:(NSString *)text
                  viewHash:(NSUInteger)hash {
    return [NSString stringWithFormat:@"%@|%@|%lu",
            vcClass ?: @"", text ?: @"", (unsigned long)hash];
}

+ (void)clearProcessed {
    // 静态类方法，此处通过实例调用
    // 实际由 WPMainHook 中的 shared instance 来完成
}

@end
