package com.lc;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;

public class BookManagerService extends Service {
    private List<Book> mBookList = new ArrayList<>();

    public BookManagerService(){}

    @Override
    public void onCreate() {
        super.onCreate();
        mBookList.add(new Book(1, "book1"));
        mBookList.add(new Book(2, "book2"));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private Binder mBinder = new IBookManager.Stub() {
        @Override
        public void addBook(Book book) throws RemoteException {
            mBookList.add(book);
        }

        @Override
        public List<Book> getBookList() throws RemoteException {
            return mBookList;
        }
    };
}
