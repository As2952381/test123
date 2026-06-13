# ============================================
# 微信红包助手 - 非越狱 dylib 构建
# 编译产物: libWeChatRedPacket.dylib
# 注入目标: WeChat.app/WeChat (脱壳版)
# ============================================

SDK ?= $(shell xcrun --sdk iphoneos --show-sdk-path)
CC  ?= $(shell xcrun --sdk iphoneos -f clang)
LD  ?= $(CC)

ARCHS       = arm64 arm64e
TARGET      = aarch64-apple-ios11.0
DYLIB_NAME  = libWeChatRedPacket.dylib
SRCS        = $(wildcard Sources/*.m)
OBJS        = $(SRCS:.m=.o)

CFLAGS  = -arch arm64 \
          -isysroot $(SDK) \
          -miphoneos-version-min=11.0 \
          -fobjc-arc \
          -fvisibility=hidden \
          -O2 \
          -Wall \
          -I./Sources

LDFLAGS = -arch arm64 \
          -isysroot $(SDK) \
          -miphoneos-version-min=11.0 \
          -dynamiclib \
          -install_name @rpath/$(DYLIB_NAME) \
          -framework Foundation \
          -framework UIKit \
          -framework CoreGraphics \
          -framework SystemConfiguration \
          -lobjc \
          -lc++

.PHONY: all clean

all: $(DYLIB_NAME)

$(DYLIB_NAME): $(OBJS)
	$(LD) $(LDFLAGS) -o $@ $^
	@echo "✅ $(DYLIB_NAME) 构建完成"
	@lipo -info $@

%.o: %.m
	$(CC) $(CFLAGS) -c -o $@ $<

clean:
	rm -f $(OBJS) $(DYLIB_NAME)
