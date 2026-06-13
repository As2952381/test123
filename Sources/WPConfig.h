//
//  WPConfig.h — 配置管理（NSUserDefaults）
//
#import <Foundation/Foundation.h>

@interface WPConfig : NSObject

+ (instancetype)shared;

@property (nonatomic, assign) BOOL enabled;
@property (nonatomic, assign) NSTimeInterval delayMin;
@property (nonatomic, assign) NSTimeInterval delayMax;
@property (nonatomic, assign) BOOL wifiOnly;
@property (nonatomic, copy)   NSString *excludeKeywords;

/// 判断群名是否在排除列表
- (BOOL)isGroupExcluded:(NSString *)groupName;

/// 获取随机延时
- (NSTimeInterval)randomDelay;

@end
