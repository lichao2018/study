// IBookManager.aidl
package com.lc;

import com.lc.Book;

// Declare any non-default types here with import statements

interface IBookManager {
    void addBook(in Book book);
    List<Book> getBookList();
}
