参考：

https://blog.csdn.net/linyt/article/details/42504975

# 1.下载内核源码
```
$ git clonegit://git.kernel.org/pub/scm/linux/kernel/git/torvalds/linux.git
```
# 2.安装arm的交叉编译工具链
```
$ sudo apt-get install gcc-arm-linux-gnueabi
```
# 3.编译linux内核
## a.生成vexpress开发板子的config文件
```
$ make CROSS_COMPILE=arm-linux-gnueabi- ARCH=arm vexpress_defconfig
```
## b.编译
```
$ make CROSS_COMPILE=arm-linux-gnueabi- ARCH=arm
```
生成的内核镜像位于arch/arm/boot/zImage
# 4.安装qemu模拟器
```
$ sudo apt-get install qemu-system-arm
```
# 5.制作ext3文件系统
## a.下载，编译，安装busybox
```
$ wget http://www.busybox.net/downloads/busybox-1.20.2.tar.bz2
$ tar -xzvf xxx.tar.bz
$ make defconfig
$ make CROSS_COMPILE=arm-linux-gnueabi-
$ make install CROSS_COMPILE=arm-linux-gnueabi-
```
安装编译完成后，在busybox下会生成_install目录
## b.形成根目录结构，创建rootfs根目录
```
$ mkdir -p rootfs/{dev,etc/init.d,lib,sys,proc}
```
## c.拷贝busybox命令到根目录下
```
$ sudo cp busybox-1.20.2/_install/* -r rootfs/
```
## d.从工具链中拷贝运行库到lib目录下
```
$ sudo cp -P /usr/arm-linux-gnueabi/lib/* rootfs/lib/
```
## e.在etc/init.d下创建启动文件rcS，系统启动时会运行到该文件，目前里面打印了些日志，没有该文件系统启动时会报错
```
echo "-----------------"
echo "hello world"
echo "-----------------"
```
## e.创建4个tty终端设备
```
$ sudo mknod rootfs/dev/tty1 c 4 1
$ sudo mknod rootfs/dev/tty2 c 4 2
$ sudo mknod rootfs/dev/tty3 c 4 3
$ sudo mknod rootfs/dev/tty4 c 4 4
```
## f.制作根文件系统镜像
//生成32M大小的镜像
```
$ dd if=/dev/zero of=a9rootfs.ext3 bs=1M count=32
```
//格式化成ext3文件系统
```
$ mkfs.ext3 a9rootfs.ext3
```
//将文件拷贝到镜像中
```
$ sudo mkdir tmpfs
$ sudo mount -t ext3 a9rootfs.ext3 tmpfs/ -o loop
$ sudo cp -r rootfs/*  tmpfs/
$ sudo umount tmpfs
```
# 6.启动运行qemu
```
$ qemu-system-arm -M vexpress-a9 -m 512M -kernel /linux-3.0/arch/arm/boot/zImage -nographic -append "root=/dev/mmcblk0  console=ttyAMA0" -sd a9rootfs.ext3
```
-M表示板子类型
-m表示运行物理内存
-kernel表示内核镜像位置
-nographic表示不使用图形化界面，只使用串口
-append 内核启动参数
