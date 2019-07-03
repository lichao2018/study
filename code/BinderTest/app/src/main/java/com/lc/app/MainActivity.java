package com.lc.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.lc.Book;
import com.lc.IBookManager;

import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {
    IBookManager mBookManager;
    List<Book> mBookList;
    TextView tvResult;
    EditText etName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        bindLcService();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btn_addbook){
            addBook();
        }
        if(v.getId() == R.id.btn_get_book_list){
            getBookList();
        }
    }

    void initView(){
        tvResult = findViewById(R.id.tv_result);
        etName = findViewById(R.id.et_book_name);
        findViewById(R.id.btn_addbook).setOnClickListener(this);
        findViewById(R.id.btn_get_book_list).setOnClickListener(this);
    }

    void bindLcService(){
        Intent intent = new Intent();
        intent.setAction("com.lc.AIDL_TEST");
        intent.setPackage("com.lc.bindertest");
        if(!getApplicationContext().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)){
            Toast.makeText(this, "bind service fail", Toast.LENGTH_SHORT).show();
        }
    }

    void addBook(){
        if(mBookList == null){
            Toast.makeText(this, "book list null", Toast.LENGTH_SHORT).show();
            return;
        }
        Book book = new Book(mBookList.size() + 1, etName.getText().toString());
        mBookList.add(book);
        Toast.makeText(this, "add success", Toast.LENGTH_SHORT).show();
    }

    void getBookList(){
        if(mBookList == null){
            Toast.makeText(this, "book list null", Toast.LENGTH_SHORT).show();
            return;
        }
        for(Book book : mBookList){
            tvResult.append(book.getId() + " " + book.getName() + "\n");
        }
    }

    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBookManager = IBookManager.Stub.asInterface(service);
            try {
                mBookList = mBookManager.getBookList();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
}
