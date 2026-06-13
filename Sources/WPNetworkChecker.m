//
//  WPNetworkChecker.m — WiFi 检测实现
//
#import "WPNetworkChecker.h"
#import "WPCommon.h"
#import "WPConfig.h"
#import <SystemConfiguration/SystemConfiguration.h>

@implementation WPNetworkChecker

+ (BOOL)isWifiConnected {
    // 使用 SystemConfiguration 检测网络类型（非越狱友好）
    // 兼容 iOS 11+
    NSArray *interfaceNames = CFBridgingRelease(
        CNCopySupportedInterfaces());
    if (!interfaceNames) return NO;

    for (NSString *ifName in interfaceNames) {
        NSDictionary *info = CFBridgingRelease(
            CNCopyCurrentNetworkInfo((__bridge CFStringRef)ifName));
        if (info && info[(__bridge NSString *)kCNNetworkInfoKeySSID]) {
            // 有 SSID = WiFi 连接
            return YES;
        }
    }
    return NO;
}

+ (BOOL)shouldProceed {
    WPConfig *cfg = [WPConfig shared];
    if (!cfg.wifiOnly) return YES;
    return [self isWifiConnected];
}

@end
