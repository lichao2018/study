# Handler机制

handler允许你发送和处理与线程MessageQueue关联的消息和任务。

## 一.作用

Android应用是一个死循环，在这个循环里面顺序地处理消息，没有消息时，循环被阻塞，直到应用退出的过程。

Handler是用来发送消息和处理消息的类。


## 二.相关类

### 1.Looper

循环取消息的类。一个Thread对应一个Looper。

### 2.MessageQueue

消息队列，按时间顺序存储消息，供Looper来取的类。一个Thread对应一个MessageQueue。

### 3.Message

消息，存储消息或任务的媒介。


## 二.运作流程

### 1.整体流程

Android应用入口ActivityThread的main方法中，调用了Looper.prepareMainLooper()和Looper.loop()方法，prepare中新建了Looper对象，并将其放到当前线程中。Looper的构造函数中新建了MessageQueue对象。loop开启了循环。
<font color=red>(线程threadlocal研究下)</font>
```
public static final void prepare() {
    if (sThreadLocal.get() != null) {
        throw new RuntimeException("Only one Looper may be created per thread");
    }
    sThreadLocal.set(new Looper());
}

private Looper(boolean quitAllowed) {
    mQueue = new MessageQueue(quitAllowed);
    mThread = Thread.currentThread();
}
```

### 2.loop分析

MessageQueue是一个链表，loop中调用MessageQueue的next获取下一条Message。
```
public static void loop() {
    for (;;) {
        Message msg = queue.next(); // might block
        msg.target.dispatchMessage(msg);
    }
}
```

### 3.next分析

进入next方法后，有一个循环，这个循环是为了在指定时间给loop返回message，这个时间是什么时间呢？handler在发送消息进入队列时，会将入队列的时间赋予给Message的when参数，如果是延时发送的消息，就加上延时的时间，然后赋予when。
所以next方法中，如果Message的when大于当前时间now，就要回到for循环的开头在调用nativePollOnce阻塞when-now个时间，如果队列中没有Message，就阻塞-1个时间，就会一直阻塞下去。直到有消息入队列时，调用了nativeWake，才会被唤醒。
```
Message next() {
    int nextPollTimeoutMillis = 0;
    for (;;) {
        nativePollOnce(ptr, nextPollTimeoutMillis);
        synchronized (this) {
            // Try to retrieve the next message.  Return if found.
            final long now = SystemClock.uptimeMillis();
            Message prevMsg = null;
            Message msg = mMessages;
            if (msg != null) {
                if (now < msg.when) {
                    // Next message is not ready.  Set a timeout to wake up when it is ready.
                    nextPollTimeoutMillis = (int) Math.min(msg.when - now, Integer.MAX_VALUE);
                } else {
                    // Got a message.
                    mBlocked = false;
                    if (prevMsg != null) {
                        prevMsg.next = msg.next;
                    } else {
                        mMessages = msg.next;
                    }
                    msg.next = null;
                    if (DEBUG) Log.v(TAG, "Returning message: " + msg);
                    msg.markInUse();
                    return msg;
                }
            } else {
                // No more messages.
                nextPollTimeoutMillis = -1;
            }
        }
    }
}
```

### 4.sendMessage分析

sendMessage方法最终会调用到MessageQueue的enqueueMessage，并将入队列时间+延时时间(0或者其他)传入，将Message插入队列中时，是按该时间参数排列。如果队列中没有任务处于阻塞状态，则调nativeWake唤醒。
```
public final boolean sendMessage(Message msg)
{
    return sendMessageDelayed(msg, 0);
}

public final boolean sendMessageDelayed(Message msg, long delayMillis)
{
    return sendMessageAtTime(msg, SystemClock.uptimeMillis() + delayMillis);
}
    
public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
    MessageQueue queue = mQueue;
    if (queue == null) {
        RuntimeException e = new RuntimeException(
                this + " sendMessageAtTime() called with no mQueue");
        Log.w("Looper", e.getMessage(), e);
        return false;
    }
    return enqueueMessage(queue, msg, uptimeMillis);
}
    
private boolean enqueueMessage(MessageQueue queue, Message msg, long uptimeMillis) {
    msg.target = this;
    if (mAsynchronous) {
        msg.setAsynchronous(true);
    }
    return queue.enqueueMessage(msg, uptimeMillis);
}

boolean enqueueMessage(Message msg, long when) {
    if (msg.target == null) {
        throw new IllegalArgumentException("Message must have a target.");
    }
    synchronized (this) {
        msg.when = when;
        Message p = mMessages;
        boolean needWake;
        if (p == null || when == 0 || when < p.when) {
            // New head, wake up the event queue if blocked.
            msg.next = p;
            mMessages = msg;
            needWake = mBlocked;
        } else {
            // Inserted within the middle of the queue.  Usually we don't have to wake
            // up the event queue unless there is a barrier at the head of the queue
            // and the message is the earliest asynchronous message in the queue.
            needWake = mBlocked && p.target == null && msg.isAsynchronous();
            Message prev;
            for (;;) {
                prev = p;
                p = p.next;
                if (p == null || when < p.when) {
                    break;
                }
                if (needWake && p.isAsynchronous()) {
                    needWake = false;
                }
            }
            msg.next = p; // invariant: p == prev.next
            prev.next = msg;
        }

        // We can assume mPtr != 0 because mQuitting is false.
        if (needWake) {
            nativeWake(mPtr);
        }
    }
    return true;
}
```

post方法里面也是调用了sendMessageDelayed方法，不过是将Runnable的run方法作为回调赋予给了Message的callback属性。
```
public final boolean post(Runnable r)
{
    return sendMessageDelayed(getPostMessage(r), 0);
}

private static Message getPostMessage(Runnable r) {
    Message m = Message.obtain();
    m.callback = r;
    return m;
}
```

### 5.处理消息

在Handler的enqueueMessage方法中，把this赋予给了Message的target属性。在Looper的loop方法中，会调用msg.target.dispatchMessage方法处理消息，也就是又交给handler处理了。
如果msg.callback不空，就调用msg.callback.run，也就是post的runnable。
```
public void dispatchMessage(Message msg) {
    if (msg.callback != null) {
        handleCallback(msg);
    } else {
        if (mCallback != null) {
            if (mCallback.handleMessage(msg)) {
                return;
            }
        }
        handleMessage(msg);
    }
}

private static void handleCallback(Message message) {
    message.callback.run();
}
```


## 三.常见问题

### 1.Message一般不推荐直接new一个对象，而是建议使用Message.obtain获取，为什么？

于Message对象，一般并不推荐直接使用它的构造方法得到，而是建议通过使用Message.obtain()这个静态的方法或者Handler.obtainMessage()获取。Message.obtain()会从消息池中获取一个Message对象，如果消息池中是空的，才会使用构造方法实例化一个新Message，这样有利于消息资源的利用。并不需要担心消息池中的消息过多，它是有上限的，上限为10个。

### 2.Handler造成的内存泄漏问题？

在Java中，非静态的内部类和匿名内部类都会隐式地持有其外部类的引用。静态的内部类不会持有外部类的引用。 解决方式 要解决这种问题，思路就是不适用非静态内部类，继承Handler时，要么是放在单独的类文件中，要么就是使用静态内部类。因为静态的内部类不会持有外部类的引用，所以不会导致外部类实例的内存泄露。当你需要在静态内部类中调用外部的Activity时，我们可以使用弱引用来处理。另外关于同样也需要将Runnable设置为静态的成员属性。注意：一个静态的匿名内部类实例不会持有外部类的引用。

### 3.为什么looper阻塞了主线程，但是没有卡死？
