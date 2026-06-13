//
//  WPNetworkChecker.h — WiFi 检测
//
#import <Foundation/Foundation.h>

@interface WPNetworkChecker : NSObject

/// 当前是否连接 WiFi
+ (BOOL)isWifiConnected;

/// 根据配置判断是否应该继续
+ (BOOL)shouldProceed;

@end
