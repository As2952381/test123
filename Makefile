# ============================================
# 微信红包助手 - 非越狱 dylib 构建
# ============================================

SDK      != xcrun --sdk iphoneos --show-sdk-path
CC       = xcrun --sdk iphoneos clang
DYLIB    = libWeChatRedPacket.dylib
SRCS     != ls Sources/*.m
OBJS     = $(SRCS:.m=.o)

CFLAGS   = -arch arm64 \
           -isysroot $(SDK) \
           -miphoneos-version-min=11.0 \
           -fobjc-arc \
           -fvisibility=hidden \
           -O2 \
           -Wall \
           -I./Sources

LDFLAGS  = -arch arm64 \
           -isysroot $(SDK) \
           -miphoneos-version-min=11.0 \
           -dynamiclib \
           -install_name @rpath/$(DYLIB) \
           -framework Foundation \
           -framework UIKit \
           -framework CoreGraphics \
           -framework SystemConfiguration

.PHONY: all clean

all: $(DYLIB)

$(DYLIB): $(OBJS)
	$(CC) $(LDFLAGS) -o $@ $^
	@echo "Build done: $(DYLIB)"
	@lipo -info $@ 2>/dev/null || file $@

%.o: %.m
	$(CC) $(CFLAGS) -c -o $@ $<

clean:
	rm -f $(OBJS) $(DYLIB)
