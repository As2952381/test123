//
//  WPRedPacketDetector.h — View 树遍历 + 红包检测
//
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

@class WPAutoClicker;

@interface WPRedPacketDetector : NSObject

- (instancetype)initWithClicker:(WPAutoClicker *)clicker;

/// 遍历 view 树，检测并自动点击红包
/// @param view 根 view
/// @param vcName 当前 ViewController 类名
/// @return 发现的目标数
- (NSInteger)scanView:(UIView *)view vcName:(NSString *)vcName;

// ── 暴露给测试的方法 ──
- (NSString *)textOfView:(UIView *)view;
- (BOOL)isOpenButtonText:(NSString *)text;
- (BOOL)isRedPacketText:(NSString *)text;
- (BOOL)isRedPacketColor:(UIColor *)color;

@end
