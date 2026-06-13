//
//  WPConfig.m — 配置管理实现
//
#import "WPConfig.h"
#import "WPCommon.h"
#import <UIKit/UIKit.h>

@implementation WPConfig

+ (instancetype)shared {
    static WPConfig *instance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        instance = [[WPConfig alloc] init];
    });
    return instance;
}

- (instancetype)init {
    self = [super init];
    if (self) {
        NSUserDefaults *def = [NSUserDefaults standardUserDefaults];
        // 注册默认值
        [def registerDefaults:@{
            kWREnabled:        @(kDefaultEnabled),
            kWDelayMin:        @(kDefaultDelayMin),
            kWDelayMax:        @(kDefaultDelayMax),
            kWWifiOnly:        @(kDefaultWifiOnly),
            kWExcludeKeywords: @"",
        }];
    }
    return self;
}

- (BOOL)enabled {
    return [[NSUserDefaults standardUserDefaults] boolForKey:kWREnabled];
}
- (void)setEnabled:(BOOL)v {
    [[NSUserDefaults standardUserDefaults] setBool:v forKey:kWREnabled];
}

- (NSTimeInterval)delayMin {
    return [[NSUserDefaults standardUserDefaults] doubleForKey:kWDelayMin];
}
- (void)setDelayMin:(NSTimeInterval)v {
    [[NSUserDefaults standardUserDefaults] setDouble:v forKey:kWDelayMin];
}

- (NSTimeInterval)delayMax {
    return [[NSUserDefaults standardUserDefaults] doubleForKey:kWDelayMax];
}
- (void)setDelayMax:(NSTimeInterval)v {
    [[NSUserDefaults standardUserDefaults] setDouble:v forKey:kWDelayMax];
}

- (BOOL)wifiOnly {
    return [[NSUserDefaults standardUserDefaults] boolForKey:kWWifiOnly];
}
- (void)setWifiOnly:(BOOL)v {
    [[NSUserDefaults standardUserDefaults] setBool:v forKey:kWWifiOnly];
}

- (NSString *)excludeKeywords {
    return [[NSUserDefaults standardUserDefaults] stringForKey:kWExcludeKeywords] ?: @"";
}
- (void)setExcludeKeywords:(NSString *)v {
    [[NSUserDefaults standardUserDefaults] setObject:v forKey:kWExcludeKeywords];
}

- (BOOL)isGroupExcluded:(NSString *)groupName {
    if (!groupName.length) return NO;
    NSString *keywords = self.excludeKeywords;
    if (!keywords.length) return NO;
    for (NSString *kw in [keywords componentsSeparatedByString:@","]) {
        NSString *trimmed = [kw stringByTrimmingCharactersInSet:
                             [NSCharacterSet whitespaceCharacterSet]];
        if (trimmed.length && [groupName containsString:trimmed]) return YES;
    }
    return NO;
}

- (NSTimeInterval)randomDelay {
    NSTimeInterval min = self.delayMin;
    NSTimeInterval max = self.delayMax;
    if (min < 0) min = 0;
    if (max < min) max = min;
    if (min == max) return min;
    return min + (NSTimeInterval)arc4random_uniform(
               (uint32_t)((max - min) * 1000)) / 1000.0;
}

@end
