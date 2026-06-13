//
//  WPViewControllerHook.h — UIViewController Method Swizzling
//
#import <Foundation/Foundation.h>

@interface WPViewControllerHook : NSObject

/// 安装所有 Hook（调用一次即可）
+ (void)install;

@end
