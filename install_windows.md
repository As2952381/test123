# Windows 用户 — iOS IPA 编译 + 安装全流程（无需 Mac）

## 整体流程

```
你在 Windows 上操作               GitHub 云端 (免费 Mac)        iPhone
─────────────────               ─────────────────────       ──────
1. 把项目推送到 GitHub
2. 触发 GitHub Actions  ──────→  macOS 编译 dylib
                                 下载脱壳 IPA
                                 注入 dylib
5. 下载 WeChat_RedPacket.ipa ←─── 打包上传
6. AltServer 签名并安装 ─────────────────────────────────→   安装完成
```

---

## 第 1 步：把项目上传到 GitHub

```powershell
# 在 WeChatRedPacket-Dylib 目录下
cd WeChatRedPacket-Dylib
git init
git add .
git commit -m "init"
gh repo create WeChatRedPacket-iOS --public --push
# 或者去 github.com 手动创建仓库然后 push
```

---

## 第 2 步：准备脱壳微信 IPA

1. 网上搜索 "微信脱壳 IPA"（常见来源：ipaom.com、decrypt.day 等）
2. 或者用一台越狱设备：CrackerXI+ / Clutch 脱壳后提取
3. 把 IPA 上传到能直链下载的地方（用 transfer.sh 或自建）

---

## 第 3 步：触发 GitHub Actions 编译

1. 打开你的 GitHub 仓库 → Actions 标签
2. 选 "编译 WeChatRedPacket + 注入 IPA"
3. 点 "Run workflow"
4. 填入脱壳 IPA 的下载链接
5. 点击运行，约 3-5 分钟完成
6. 下载产物 `WeChat_RedPacket.ipa`

---

## 第 4 步：安装到 iPhone

在 Windows 上安装 **AltServer**：

1. 下载 AltServer for Windows：https://altstore.io
2. 安装 iCloud for Windows（非 Microsoft Store 版）
3. iPhone 用数据线连电脑
4. 打开 AltServer（任务栏图标）→ Install AltStore → 选你的 iPhone
5. iPhone 上输入 Apple ID 和密码
6. iPhone 出现 AltStore 后：
   - 把 `WeChat_RedPacket.ipa` 传到 iPhone（AirDrop / QQ / 微信文件传输）
   - 用 AltStore 打开 → 安装
7. **设置 → 通用 → VPN 与设备管理 → 信任证书**

---

## 注意事项

| 问题 | 解决 |
|------|------|
| 7 天过期 | AltStore 后台会自动刷新签名（连上同一 WiFi，AltServer 开着） |
| 证书信任找不到 | 安装后重启 iPhone |
| 找不到 Apple ID 证书 | AltServer 会自动生成，只需登录 Apple ID |
| GitHub Actions 免费 | 每月 2000 分钟 macOS 额度，编译一次约 5 分钟 |

---

## 替代：买现成的

如果以上步骤嫌麻烦，淘宝 / Telegram 上有卖已注入红包插件的微信 IPA，直接买了用 AltStore 装上就行。
