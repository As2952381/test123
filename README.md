# 微信红包助手 — iOS 非越狱版

通过 dylib 注入实现的微信自动抢红包插件，**无需越狱**。

## 原理

| 步骤 | 说明 |
|------|------|
| `WPMainEntry.m` | `__attribute__((constructor))` 入口，dylib 加载时自动执行 |
| `WPViewControllerHook.m` | Swizzle `UIViewController.viewDidAppear:` → 全局拦截每个页面 |
| `WPRedPacketDetector.m` | 递归遍历 UIView 树 → 文字/颜色特征匹配红包 |
| `WPAutoClicker.m` | 随机延时 + 模拟触摸 + 防重复 |
| `WPConfig.m` | NSUserDefaults 存储配置 |
| `WPNetworkChecker.m` | WiFi 检测（仅 WiFi 模式） |

## 构建

```bash
# 需要 macOS + Xcode
cd WeChatRedPacket-Dylib
make
# 产物: libWeChatRedPacket.dylib
```

## 注入 & 安装

```bash
# 需要: 脱壳版微信 IPA + 开发者证书
./inject.sh ~/Downloads/WeChat.ipa "Apple Development: Your Name (XXXXX)"
# 产出: WeChat_RedPacket.ipa

# 用 AltStore / Sideloadly / 爱思助手 安装
```

## Hook 策略（三层降级）

| 层级 | 方法 | 描述 |
|------|------|------|
| 第1层 | Swizzle `viewDidAppear:` | 全局拦截所有页面，View 树遍历检测 |
| 第2层 | 文字 + 颜色特征 | 不依赖类名，"开"字 + 红包红 #FA5151 |
| 第3层 | 预留 | 反编译微信后可补充精确类名 Hook |

## 检测特征

- ✅ 文字："开"、"開"、"领取"、"拆红包" 等
- ✅ 颜色：微信红包红 #FA5151（含容忍度）
- ✅ 文字："红包"、"微信红包"、"拼手气红包" 等
- ✅ 防重复：`(VC类名 + 文字 + View Hash)` 唯一标识
- ✅ 随机延时：200ms ~ 800ms 模拟人类操作

## 设置

通过 URL Scheme 打开设置（需配合内置简单设置页）：
```
wcredenvelope://settings
```

或直接修改 NSUserDefaults（通过 iMazing 等工具）：
```
Key: WREnabled        (BOOL)   - 总开关
Key: WRDelayMin       (double) - 最小延时(秒)
Key: WRDelayMax       (double) - 最大延时(秒)
Key: WRWifiOnly       (BOOL)   - 仅WiFi
Key: WRExcludeKeywords (string) - 排除关键词(逗号分隔)
```

## 注意事项

1. **免费 Apple ID** 每 7 天需重签，每年最多 3 个 App
2. **企业签** 无 7 天限制，需购买
3. **开发者账号**（$99/年）永久有效，无重签限制
4. 仅支持 iOS 11.0+
5. 需要脱壳版微信 IPA（App Store 版有 FairPlay 加密，无法注入）

## 免责声明

本项目仅供学习研究使用。请勿用于非法用途。使用本软件产生的任何后果由使用者自行承担。
