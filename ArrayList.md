# ArrayList


ArrayList列表，是用数组存储数据，新建时可以指定初始容量大小，如果没指定初始容量，则初始容量为10。如果add一个元素进来时，add之后元素数量超出目前容量了，则进行扩容。
扩容是新建一个数组，大小为当前容量的1.5倍，并将现有数据使用System.arraycopy拷贝进新数组中。
```
newCapacity = oldCapacity + (oldCapacity >> 1);
//oldCapacity >> 1，相当于将oldCapacity除以2
```

插入数据时，也要用System.arraycopy方法，将index后面数据后移一位，然后将新元素插入到index位置。
```
System.arraycopy(elementData, index, elementData, index + 1, size - index);
elementData[index] = element;
```

查找数据，只是查找数组index位置的元素即可。
所以ArrayList插入，删除效率慢，查找效率快。
