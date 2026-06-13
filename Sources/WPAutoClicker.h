//
//  WPAutoClicker.h — 自动点击执行器
//
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

@interface WPAutoClicker : NSObject

/// 对目标 view 执行点击（延时 + 防重复）
/// @param view 目标 view
/// @param uniqueId 唯一标识（防重复）
/// @param desc 操作描述（日志）
/// @return 是否执行
- (BOOL)performClick:(UIView *)view
            uniqueId:(NSString *)uniqueId
               desc:(NSString *)desc;

/// 立即点击（无延时）
- (void)performClickImmediately:(UIView *)view
                       uniqueId:(NSString *)uniqueId
                           desc:(NSString *)desc;

/// 生成唯一标识
+ (NSString *)makeUniqueId:(NSString *)vcClass
                      text:(NSString *)text
                  viewHash:(NSUInteger)hash;

/// 清空已处理记录
+ (void)clearProcessed;

@end
