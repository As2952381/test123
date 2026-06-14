#!/usr/bin/env python3
"""
Windows 本地 IPA 注入脚本
用法: python inject_local.py <WeChat.ipa> <libWeChatRedPacket.dylib> [输出.ipa]
"""
import struct
import zipfile
import shutil
import sys
import os
import tempfile

def inject_dylib_into_macho(binary_path, dylib_name):
    """往 Mach-O 二进制插入 LC_LOAD_DYLIB load command"""
    with open(binary_path, 'rb') as f:
        data = f.read()

    if len(data) < 32:
        print("ERROR: Binary too small")
        return False

    magic = struct.unpack('<I', data[:4])[0]

    # 检查魔数
    if magic == 0xFEEDFACF:  # 64-bit mach-o
        header_size = 32
        is_64bit = True
        print("  64-bit Mach-O detected")
    elif magic == 0xFEEDFACE:  # 32-bit mach-o
        header_size = 28
        is_64bit = False
        print("  32-bit Mach-O detected")
    elif magic == 0xBEBAFECA:  # Universal binary
        print("  Universal binary detected, extracting arm64 slice...")
        return inject_into_fat_binary(binary_path, dylib_name)
    else:
        print(f"  Unknown magic: {hex(magic)}")
        return False

    ncmds = struct.unpack('<I', data[16:20])[0]

    # 构建 LC_LOAD_DYLIB command
    dylib_path = f'@executable_path/{dylib_name}'.encode('utf-8') + b'\x00'
    # 对齐到 8 字节
    dylib_path = dylib_path + b'\x00' * ((8 - len(dylib_path) % 8) % 8)
    lc = struct.pack('<II', 0x18, len(dylib_path) + 8) + dylib_path

    # 插入到 header 后面
    new_data = data[:header_size] + lc + data[header_size:]
    new_ncmds = ncmds + 1
    new_data = new_data[:16] + struct.pack('<I', new_ncmds) + new_data[20:]

    # 更新 header size (在 32位 offset 20, 64位 offset 24)
    if header_size == 32:
        old_hdr_size = struct.unpack('<I', new_data[20:24])[0]
        new_data = new_data[:20] + struct.pack('<I', old_hdr_size + len(lc)) + new_data[24:]

    with open(binary_path, 'wb') as f:
        f.write(new_data)

    print(f"  LC_LOAD_DYLIB injected into {binary_path}")
    return True


def inject_into_fat_binary(fat_path, dylib_name):
    """从 FAT binary 中提取 arm64 slice，注入后替换"""
    with open(fat_path, 'rb') as f:
        data = f.read()

    magic = struct.unpack('>I', data[:4])[0]
    if magic != 0xBEBAFECA:
        print("  Not a FAT binary")
        return False

    narch = struct.unpack('>I', data[4:8])[0]
    print(f"  FAT binary with {narch} architectures")

    arm64_offset = None
    arm64_size = None

    for i in range(narch):
        off = 8 + i * 20
        cpu_type = struct.unpack('>I', data[off:off+4])[0]
        cpu_sub = struct.unpack('>I', data[off+4:off+8])[0]
        slice_off = struct.unpack('>I', data[off+8:off+12])[0]
        slice_size = struct.unpack('>I', data[off+12:off+16])[0]

        arch_name = {0x0000000C: 'arm64', 0x0000000B: 'arm64e', 0x00000007: 'x86_64'}.get(cpu_type, hex(cpu_type))
        print(f"    {arch_name}: offset={slice_off}, size={slice_size}")

        if cpu_type == 0x0000000C:  # arm64
            arm64_offset = slice_off
            arm64_size = slice_size

    if arm64_offset is None:
        print("  No arm64 slice found!")
        return False

    # 提取 arm64 slice 到临时文件
    temp_path = fat_path + '.arm64'
    with open(temp_path, 'wb') as f:
        f.write(data[arm64_offset:arm64_offset + arm64_size])

    # 注入
    if not inject_dylib_into_macho(temp_path, dylib_name):
        os.remove(temp_path)
        return False

    # 替换回去
    with open(temp_path, 'rb') as f:
        new_slice = f.read()

    new_data = data[:arm64_offset] + new_slice + data[arm64_offset + arm64_size:]

    with open(fat_path, 'wb') as f:
        f.write(new_data)

    os.remove(temp_path)
    print(f"  Updated FAT binary with injected arm64 slice")
    return True


def main():
    if len(sys.argv) < 3:
        print("Usage: python inject_local.py <WeChat.ipa> <libWeChatRedPacket.dylib> [output.ipa]")
        print("Example: python inject_local.py WeChat.ipa libWeChatRedPacket.dylib WeChat_RedPacket.ipa")
        sys.exit(1)

    ipa_path = sys.argv[1]
    dylib_path = sys.argv[2]
    output_ipa = sys.argv[3] if len(sys.argv) > 3 else 'WeChat_RedPacket.ipa'

    for f in [ipa_path, dylib_path]:
        if not os.path.exists(f):
            print(f"ERROR: File not found: {f}")
            sys.exit(1)

    dylib_name = os.path.basename(dylib_path)

    print("=" * 50)
    print("  WeChat RedPacket Injector (Windows)")
    print("=" * 50)
    print(f"  IPA:   {ipa_path}")
    print(f"  dylib: {dylib_path}")
    print(f"  Output: {output_ipa}")
    print()

    work_dir = tempfile.mkdtemp(prefix='wcinject_')

    try:
        # 1. 解压 IPA
        print("[1/4] Extracting IPA...")
        with zipfile.ZipFile(ipa_path, 'r') as zf:
            zf.extractall(work_dir)

        # 2. 找 .app
        payload = os.path.join(work_dir, 'Payload')
        app_dir = None
        for f in os.listdir(payload):
            if f.endswith('.app'):
                app_dir = os.path.join(payload, f)
                break

        if not app_dir:
            print("ERROR: No .app found in IPA")
            sys.exit(1)

        app_name = os.path.basename(app_dir).replace('.app', '')
        print(f"  Found: {app_name}.app")

        # 3. 复制 dylib
        print("[2/4] Copying dylib...")
        shutil.copy2(dylib_path, os.path.join(app_dir, dylib_name))
        print(f"  {dylib_name} -> {app_name}.app/")

        # 4. 注入
        print("[3/4] Injecting dylib into binary...")
        binary_path = os.path.join(app_dir, app_name)
        if not os.path.exists(binary_path):
            print(f"ERROR: Binary not found: {binary_path}")
            sys.exit(1)

        inject_dylib_into_macho(binary_path, dylib_name)

        # 5. 重新打包
        print("[4/4] Repacking IPA...")
        with zipfile.ZipFile(output_ipa, 'w', zipfile.ZIP_DEFLATED) as zf:
            for root, dirs, files in os.walk(work_dir):
                for fn in files:
                    full = os.path.join(root, fn)
                    arcname = os.path.relpath(full, work_dir)
                    zf.write(full, arcname)

        print(f"\n  DONE: {output_ipa}")
        print(f"  Install with AltStore / Sideloadly / 爱思助手")

    finally:
        shutil.rmtree(work_dir)


if __name__ == '__main__':
    main()
