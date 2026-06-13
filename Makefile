SDK      = $(shell xcrun --sdk iphoneos --show-sdk-path 2>/dev/null)
CC       = $(shell xcrun --sdk iphoneos -f clang 2>/dev/null)
DYLIB    = libWeChatRedPacket.dylib
SRCS     = $(wildcard Sources/*.m)
OBJS     = $(SRCS:.m=.o)

CFLAGS   = -arch arm64 -isysroot $(SDK) -miphoneos-version-min=11.0 -fobjc-arc -fvisibility=hidden -O2 -Wall -I./Sources
LDFLAGS  = -arch arm64 -isysroot $(SDK) -miphoneos-version-min=11.0 -dynamiclib -install_name @rpath/$(DYLIB) -framework Foundation -framework UIKit -framework CoreGraphics -framework SystemConfiguration

.PHONY: all clean

all: $(DYLIB)

$(DYLIB): $(OBJS)
	$(CC) $(LDFLAGS) -o $@ $^
	@echo "Build: $(DYLIB)"
	@lipo -info $@ 2>/dev/null || file $@

%.o: %.m
	$(CC) $(CFLAGS) -c -o $@ $<

clean:
	rm -f $(OBJS) $(DYLIB)
