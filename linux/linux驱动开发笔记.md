参考：

https://www.bilibili.com/video/BV1YJ41187fj?p=1

## 驱动编写
hello.c
```
#include <linux/kernel.h>
#include <linux/module.h>
#include <linux/init.h>

int __init xxx_init(void)
{
    /*这里是模块加载时的初始化工作*/
    printk(KERN_INFO "Hello World\n");
    return 0;
}

void __exit xxx_exit(void)
{
    /*这里是模块卸载时的销毁工作*/
    printk(KERN_INFO "Goodbye World\n");
}
module_init(xxx_init);                        /*指定模块的初始化函数的宏*/
module_exit(xxx_exit);                        /*指定模块的卸载函数的宏*/
MODULE_LICENSE("GPL");
```

## 驱动编译
驱动编译分为：
内部编译（内核源码内进行编译）
静态编译（编进uImage，使用Kconfig，Make file，make menuconfig）
外部编译（内核外编译）
动态编译（编译成ko）
### 编写编译驱动的Makefile

源码下有makefile编写介绍：Documentation/kbuild/makefiles.txt

**注意：**

写Makefile时，红色背景的行表示报错
不能用空格，要用tab代替，否则make时报错：
```
make: Nothing to be done for `all'
```
示例：
```
obj-m := hello.o

KERNDIR:= /lib/modules/$(shell uname -r)/build/
PWD:=$(shell pwd)

module:
        make -C $(KERNDIR) M=$(PWD) modules

clean:
        make -C $(KERNDIR) M=$(PWD) clean
```
### 编译
调用make指令即可调用

## 驱动使用
modinfo 查看模块信息
lsmod 查看当前内核已经插入的动态模块
insmod 装载模块
dmesg 查看装载日志 -c 清除内核日志信息
rmmod 卸载模块

## 字符设备驱动
需要的结构体：
```
struct cdev{
    struct module *owner;
    const struct file_operations *ops;  //提供给应用的操作方法集
    dev_t dev;                          //设备号
    unsigned int count;                 //设备个数
    struct list_head list;
}

struct file_operations{
    *read();
    *write();
    *open();
    ...
}
```

### 编写字符设备驱动
1. 分配设备号
```
    int alloc_chrdev_region();       //自动分配
    int register_chrdev_region();    //指定分配
```
2. 注销设备号
```
    void unregister_chrdev_region();
```
3. 给cdev结构体分配内存空间
```
    struct cdev *cdev_alloc(void)
```
4. 初始化cdev结构体
```
    void cdev_init()
```
5. 注册字符设备到内核，由内核统一管理
```
    int cdev_add()
```
6. 注销设备
```
    void cdev_del()
```

示例：
```
#include <linux/kernel.h>
#include <linux/module.h>
#include <linux/init.h>
#include <linux/cdev.h>
#include <linux/fs.h>

#define BASEMINOR 0
#define COUNT     3
#define NAME      "chrdev_hello"

dev_t devno;
struct cdev *cdevp = NULL;

int hello_open(struct inode *inode, struct file *filp){
    printk(KERN_INFO "hello_open");
    return 0;
}

int hello_release(struct inode *inode, struct file *filp){
    printk(KERN_INFO "hello_release");
    return 0;
}

struct file_operations fps = {
    .owner =   THIS_MODULE,
    .open =    hello_open,
    .release = hello_release,
};

int __init hello_init(void)
{
    int ret;
    //分配设备号
    ret = alloc_chrdev_region(&devno, BASEMINOR, COUNT, NAME);
    if(ret < 0){
	printk(KERN_ERR "alloc_chrdev failed\n");
	goto err1;
    }
    printk(KERN_INFO "major = %d \n", MAJOR(devno));
    //给cdev分配空间
    cdevp = cdev_alloc();
    if(cdevp == NULL){
        printk(KERN_ERR "cdev_alloc failed\n");
	ret = -ENOMEM;
	goto err2;
    }
    //cdev初始化
    cdev_init(cdevp, &fps);
    //注册设备
    ret = cdev_add(cdevp, devno, COUNT);
    if(ret < 0){
	printk(KERN_ERR "cdev add");
	goto err2;
    }

    /*这里是模块加载时的初始化工作*/
    printk(KERN_INFO "Hello World\n");
    return 0;

err2:
    unregister_chrdev_region(devno, COUNT);
err1:
    return ret;
}

void __exit hello_exit(void)
{
    cdev_del(cdevp);
    unregister_chrdev_region(devno, COUNT);
    /*这里是模块卸载时的销毁工作*/
    printk(KERN_INFO "Goodbye World\n");
}
module_init(hello_init);                        /*指定模块的初始化函数的宏*/
module_exit(hello_exit);                        /*指定模块的卸载函数的宏*/
MODULE_LICENSE("GPL");
```
