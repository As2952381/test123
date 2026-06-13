#!/bin/bash
# ============================================
# 微信红包助手 - IPA 注入 + 重签脚本
# 用法: ./inject.sh <脱壳微信IPA路径> [证书名称]
# ============================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

IPA_PATH="$1"
CERT_NAME="${2:-Apple Development: YourName (XXXXXXXXXX)}"
WORK_DIR="./_work"
DYLIB="libWeChatRedPacket.dylib"

echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  微信红包助手 - IPA 注入工具${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

if [ -z "$IPA_PATH" ]; then
    echo -e "${RED}错误: 请提供脱壳微信 IPA 路径${NC}"
    echo "用法: $0 <WeChat.ipa> [证书名称]"
    echo ""
    echo "准备工作:"
    echo "  1. 下载脱壳版微信 IPA（网上搜索或自行脱壳）"
    echo "  2. 安装 optool 或 insert_dylib: brew install optool"
    echo "  3. 查看可用证书: security find-identity -v -p codesigning"
    exit 1
fi

if [ ! -f "$IPA_PATH" ]; then
    echo -e "${RED}错误: IPA 文件不存在: $IPA_PATH${NC}"
    exit 1
fi

if [ ! -f "$DYLIB" ]; then
    echo -e "${YELLOW}dylib 未编译，开始构建...${NC}"
    make clean && make
    if [ ! -f "$DYLIB" ]; then
        echo -e "${RED}构建失败${NC}"
        exit 1
    fi
fi

# 清理
rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR"

# ── 1. 解压 IPA ──
echo -e "${YELLOW}[1/5] 解压 IPA...${NC}"
unzip -q "$IPA_PATH" -d "$WORK_DIR"

# 找到 .app 目录
APP_DIR=$(find "$WORK_DIR/Payload" -name "*.app" -type d | head -1)
if [ -z "$APP_DIR" ]; then
    echo -e "${RED}错误: 在 IPA 中找不到 .app 目录${NC}"
    exit 1
fi
echo "  找到: $APP_DIR"

# ── 2. 复制 dylib ──
echo -e "${YELLOW}[2/5] 复制 dylib...${NC}"
cp "$DYLIB" "$APP_DIR/"
echo "  $DYLIB → $APP_DIR/"

# ── 3. 注入 dylib 到二进制 ──
echo -e "${YELLOW}[3/5] 注入 dylib 到 WeChat 二进制...${NC}"
BINARY="$APP_DIR/$(basename "$APP_DIR" .app)"

# 用 insert_dylib 或 optool
if command -v insert_dylib &> /dev/null; then
    insert_dylib "@executable_path/$DYLIB" "$BINARY" --all-yes
    echo "  使用 insert_dylib 注入成功"
elif command -v optool &> /dev/null; then
    optool install -c load -p "@executable_path/$DYLIB" -t "$BINARY"
    echo "  使用 optool 注入成功"
else
    echo -e "${RED}错误: 未找到 insert_dylib 或 optool${NC}"
    echo "安装方法: brew install optool"
    rm -rf "$WORK_DIR"
    exit 1
fi

# ── 4. 重签 ──
echo -e "${YELLOW}[4/5] 重新签名...${NC}"
# 先签 Frameworks
find "$APP_DIR/Frameworks" -name "*.framework" -maxdepth 1 | while read fw; do
    /usr/bin/codesign --force --sign "$CERT_NAME" --timestamp=none "$fw" 2>/dev/null || true
done
# 签 dylib 和 app
/usr/bin/codesign --force --sign "$CERT_NAME" --timestamp=none "$APP_DIR/$DYLIB" 2>/dev/null
/usr/bin/codesign --force --sign "$CERT_NAME" --timestamp=none --entitlements entitlements.plist "$APP_DIR" 2>/dev/null

# ── 5. 重新打包 IPA ──
echo -e "${YELLOW}[5/5] 重新打包 IPA...${NC}"
OUTPUT_IPA="WeChat_RedPacket.ipa"
cd "$WORK_DIR"
zip -qr "../$OUTPUT_IPA" Payload/
cd ..
echo -e "${GREEN}✅ 完成: $OUTPUT_IPA${NC}"

# 清理
rm -rf "$WORK_DIR"
echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  下一步:${NC}"
echo -e "  1. 用 ${YELLOW}AltStore${NC} / ${YELLOW}Sideloadly${NC} / 企业签安装 $OUTPUT_IPA"
echo -e "  2. 信任证书: 设置 → 通用 → VPN与设备管理"
echo -e "  3. 打开微信，会自动启用抢红包"
echo -e ""
echo -e "  设置项 (通过 URL Scheme):"
echo -e "  ${YELLOW}wcredenvelope://settings${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
