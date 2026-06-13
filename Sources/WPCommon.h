//
//  WPCommon.h — 公共定义
//
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

// ═══════════════════════════════════════════
// 配置 Key（存储在 NSUserDefaults）
// ═══════════════════════════════════════════
static NSString * const kWREnabled        = @"WREnabled";
static NSString * const kWDelayMin        = @"WRDelayMin";
static NSString * const kWDelayMax        = @"WRDelayMax";
static NSString * const kWWifiOnly        = @"WRWifiOnly";
static NSString * const kWExcludeKeywords = @"WRExcludeKeywords";

static const BOOL    kDefaultEnabled   = YES;
static const NSTimeInterval kDefaultDelayMin = 0.2;
static const NSTimeInterval kDefaultDelayMax = 0.8;
static const BOOL    kDefaultWifiOnly  = NO;

// 红包检测关键词
static NSString * const kRedPacketKeywords[] = {
    @"红包", @"微信红包", @"发红包", @"领取红包",
    @"拼手气红包", @"普通红包", @"专属红包",
    @"red packet", @"lucky money", nil
};

// "开"按钮关键词
static NSString * const kOpenKeywords[] = {
    @"开", @"開", @"领取", @"领取红包", @"拆红包", @"点我领取", nil
};

// 微信红包红 #FA5151
static const CGFloat kTargetRedR = 0.980;
static const CGFloat kTargetRedG = 0.318;
static const CGFloat kTargetRedB = 0.318;
static const CGFloat kColorTolerance = 0.12;

// ═══════════════════════════════════════════
// 日志宏
// ═══════════════════════════════════════════
#ifdef DEBUG
#define WPLog(fmt, ...) NSLog(@"[WeChatRedPacket] " fmt, ##__VA_ARGS__)
#else
#define WPLog(fmt, ...)
#endif
