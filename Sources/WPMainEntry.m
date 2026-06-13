//
//  WPMainEntry.m — Dylib 入口
//
//  __attribute__((constructor)) 确保 dylib 加载时自动执行
//  等效于 Xposed 的 handleLoadPackage
//
#import "WPViewControllerHook.h"
#import "WPCommon.h"
#import <UIKit/UIKit.h>
#import <dlfcn.h>

/// Dylib 加载时自动调用（在 main() 之前）
__attribute__((constructor))
static void WeChatRedPacketInit(void) {
    @autoreleasepool {
        // 确认当前进程是微信
        NSString *bundleId = [[NSBundle mainBundle] bundleIdentifier];
        if (![bundleId isEqualToString:@"com.tencent.xin"]) {
            // 不是微信进程，静默退出
            // （理论上不会到这里——plist filter 已限制注入范围）
            return;
        }

        WPLog(@"========================================");
        WPLog(@"微信红包助手 Dylib 已加载");
        WPLog(@"Bundle: %@", bundleId);
        WPLog(@"版本: 1.0.0 (非越狱)");
        WPLog(@"========================================");

        // 安装 Hook
        [WPViewControllerHook install];

        WPLog(@"所有 Hook 安装完毕，等待红包...");
    }
}
