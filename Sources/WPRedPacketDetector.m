//
//  WPRedPacketDetector.m — View 树遍历 + 红包检测实现
//
#import "WPRedPacketDetector.h"
#import "WPAutoClicker.h"
#import "WPCommon.h"
#import "WPConfig.h"

@implementation WPRedPacketDetector {
    WPAutoClicker *_clicker;
    NSInteger _foundCount;
}

- (instancetype)initWithClicker:(WPAutoClicker *)clicker {
    self = [super init];
    if (self) {
        _clicker = clicker;
    }
    return self;
}

- (NSInteger)scanView:(UIView *)rootView vcName:(NSString *)vcName {
    _foundCount = 0;
    [self traverseView:rootView vcName:vcName];
    return _foundCount;
}

// ═══════════════════════════════════════════
// 递归遍历
// ═══════════════════════════════════════════
- (void)traverseView:(UIView *)view vcName:(NSString *)vcName {
    if (!view) return;
    if (view.hidden || view.alpha < 0.1) return;  // 跳过不可见

    BOOL wasTarget = [self checkTarget:view vcName:vcName];
    if (wasTarget) _foundCount++;

    for (UIView *sub in view.subviews) {
        [self traverseView:sub vcName:vcName];
    }
}

// ═══════════════════════════════════════════
// 目标判定
// ═══════════════════════════════════════════
- (BOOL)checkTarget:(UIView *)view vcName:(NSString *)vcName {
    NSString *text = [self textOfView:view];

    // 1) "开"按钮 — 文字匹配 + 颜色检查
    if ([self isOpenButtonText:text]) {
        // 检查背景色
        if ([self hasRedBackgroundColor:view]) {
            NSString *uid = [WPAutoClicker makeUniqueId:vcName
                                                   text:text
                                               viewHash:[view hash]];
            return [_clicker performClick:view uniqueId:uid
                                     desc:[NSString stringWithFormat:@"开按钮: %@", text]];
        }
        // 文字就是"开"且可交互
        if (([text isEqualToString:@"开"] || [text isEqualToString:@"開"])
            && view.userInteractionEnabled) {
            NSString *uid = [WPAutoClicker makeUniqueId:vcName
                                                   text:text
                                               viewHash:[view hash]];
            return [_clicker performClick:view uniqueId:uid
                                     desc:[NSString stringWithFormat:@"开按钮(纯文字): %@", text]];
        }
    }

    // 2) 红包文字
    if ([self isRedPacketText:text] && view.userInteractionEnabled) {
        NSString *uid = [WPAutoClicker makeUniqueId:vcName
                                               text:text
                                           viewHash:[view hash]];
        return [_clicker performClick:view uniqueId:uid
                                 desc:[NSString stringWithFormat:@"红包文案: %@", text]];
    }

    // 3) 红色背景 + 可交互
    if ([self hasRedBackgroundColor:view] && view.userInteractionEnabled) {
        NSString *uid = [WPAutoClicker makeUniqueId:vcName
                                               text:text
                                           viewHash:[view hash]];
        return [_clicker performClick:view uniqueId:uid
                                 desc:[NSString stringWithFormat:@"红色View: %@", text]];
    }

    return NO;
}

// ═══════════════════════════════════════════
// 文字提取
// ═══════════════════════════════════════════
- (NSString *)textOfView:(UIView *)view {
    NSMutableString *result = [NSMutableString string];

    // UILabel
    if ([view isKindOfClass:[UILabel class]]) {
        UILabel *label = (UILabel *)view;
        if (label.text.length) [result appendString:label.text];
        if (label.attributedText.string.length)
            [result appendString:label.attributedText.string];
    }
    // UIButton
    if ([view respondsToSelector:@selector(titleLabel)]) {
        UILabel *titleLabel = [view performSelector:@selector(titleLabel)];
        if (titleLabel.text.length) [result appendString:titleLabel.text];
    }
    // accessibilityLabel
    if (view.accessibilityLabel.length)
        [result appendString:view.accessibilityLabel];
    // accessibilityValue
    if (view.accessibilityValue.length)
        [result appendString:view.accessibilityValue];

    return [result stringByTrimmingCharactersInSet:
            [NSCharacterSet whitespaceAndNewlineCharacterSet]];
}

// ═══════════════════════════════════════════
// 文字判断
// ═══════════════════════════════════════════
- (BOOL)isOpenButtonText:(NSString *)text {
    if (!text.length) return NO;
    NSString *lower = [text lowercaseString];
    for (int i = 0; kOpenKeywords[i]; i++) {
        if ([lower containsString:[kOpenKeywords[i] lowercaseString]])
            return YES;
    }
    return NO;
}

- (BOOL)isStrictOpenChar:(NSString *)text {
    return [text isEqualToString:@"开"] || [text isEqualToString:@"開"];
}

- (BOOL)isRedPacketText:(NSString *)text {
    if (!text.length) return NO;
    NSString *lower = [text lowercaseString];
    for (int i = 0; kRedPacketKeywords[i]; i++) {
        if ([lower containsString:[kRedPacketKeywords[i] lowercaseString]])
            return YES;
    }
    return NO;
}

// ═══════════════════════════════════════════
// 颜色判断
// ═══════════════════════════════════════════
- (BOOL)hasRedBackgroundColor:(UIView *)view {
    UIColor *bg = view.backgroundColor;
    if (bg && bg != [UIColor clearColor]) {
        return [self isRedPacketColor:bg];
    }
    return NO;
}

- (BOOL)isRedPacketColor:(UIColor *)color {
    if (!color) return NO;
    CGFloat r, g, b, a;
    if (![color getRed:&r green:&g blue:&b alpha:&a]) return NO;
    if (a < 0.5) return NO;

    CGFloat diff = fabs(r - kTargetRedR)
                 + fabs(g - kTargetRedG)
                 + fabs(b - kTargetRedB);
    return diff < kColorTolerance;
}

@end
