1.内存泄漏优化，参见prepare.md

2.布局优化    
思想：尽量减少布局文件的层级，层级减少，绘制的工作就减少了。    
方法：删除无用控件和层级，使用性能较低的ViewGroup；使用<include>布局重用，<merge>减少视图层级，<ViewStub>仅在需要时加载。     
常用layout：linearlayout,tablelayout,relativelayout,framelayout,absolutelayout。    

3.绘制优化    
指View的onDraw中避免执行大量的操作。体现在(1)onDraw中不要创建新的局部对象;(2)onDraw中不要做耗时任务，不要成千上万次循环。   

4.响应速度优化   
避免在主线程中做耗时操作    

5.listview和bitmap优化   

6.线程优化   
