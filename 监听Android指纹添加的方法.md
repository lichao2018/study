如题。<br/>

用KeyStore使用AndroidKeyStore的方式存储密钥。<br/>

用Cipher初始化该密钥。<br/>

当添加指纹后，KeyStore中的密钥发生了改变，被删除了或者怎样，Cipher初始化该密钥会抛异常。<br/>

此时就监听到了Android指纹的添加。<br/>
