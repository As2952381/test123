//
//  WPNetworkChecker.m — WiFi 检测实现
//
#import "WPNetworkChecker.h"
#import "WPCommon.h"
#import "WPConfig.h"
#import <SystemConfiguration/SystemConfiguration.h>
#import <SystemConfiguration/CaptiveNetwork.h>

@implementation WPNetworkChecker

+ (BOOL)isWifiConnected {
    NSArray *interfaceNames = CFBridgingRelease(
        CNCopySupportedInterfaces());
    if (!interfaceNames) return NO;

    for (NSString *ifName in interfaceNames) {
        NSDictionary *info = CFBridgingRelease(
            CNCopyCurrentNetworkInfo((__bridge CFStringRef)ifName));
        if (info && info[(__bridge NSString *)kCNNetworkInfoKeySSID]) {
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
