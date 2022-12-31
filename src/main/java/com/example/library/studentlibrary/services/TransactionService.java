package com.example.library.studentlibrary.services;

import com.example.library.studentlibrary.models.*;
import com.example.library.studentlibrary.repositories.BookRepository;
import com.example.library.studentlibrary.repositories.CardRepository;
import com.example.library.studentlibrary.repositories.TransactionRepository;
import jdk.internal.org.objectweb.asm.util.TraceMethodVisitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TransactionService {

    @Autowired
    BookRepository bookRepository5;

    @Autowired
    CardRepository cardRepository5;

    @Autowired
    TransactionRepository transactionRepository5;

    @Value("${books.max_allowed}")
    int max_allowed_books;

    @Value("${books.max_allowed_days}")
    int getMax_allowed_days;

    @Value("${books.fine.per_day}")
    int fine_per_day;



    public String issueBook(int cardId, int bookId) throws Exception {
        //check whether bookId and cardId already exist
        //conditions required for successful transaction of issue book:

        //1. book is present and available
        // If it fails: throw new Exception("Book is either unavailable or not present");

        //2. card is present and activated
        // If it fails: throw new Exception("Card is invalid");

        //3. number of books issued against the card is strictly less than max_allowed_books
        // If it fails: throw new Exception("Book limit has reached for this card");

        //If the transaction is successful, save the transaction to the list of transactions and return the id

        String transactionId = "";
        Book book = bookRepository5.findById(bookId).get();
        if(book != null && book.isAvailable()){

            Card card = cardRepository5.findById(cardId).get();
            if(card != null && card.getCardStatus() == CardStatus.ACTIVATED){

                if(card.getBooks().size() < max_allowed_books){
                    Transaction transaction = Transaction.builder()
                            .transactionDate(new Date())
                            .book(book)
                            .card(card)
                            .isIssueOperation(true)
                            .transactionStatus(TransactionStatus.SUCCESSFUL)
                            .build();

                    List<Transaction> transactions = book.getTransactions();
                    transactions.add(transaction);
                    book.setAvailable(false);
                    book.setCard(card);
                    bookRepository5.updateBook(book);
                    transactionId = transaction.getTransactionId();
                    transactionRepository5.save(transaction);
                }else{
                    Transaction transaction = Transaction.builder()
                            .transactionDate(new Date())
                            .book(book)
                            .card(card)
                            .isIssueOperation(true)
                            .transactionStatus(TransactionStatus.FAILED)
                            .build();
                    transactionRepository5.save(transaction);
                    throw new Exception("Book limit has reached for this card");
                }

            }else{
                Transaction transaction = Transaction.builder()
                        .transactionDate(new Date())
                        .book(book)
                        .isIssueOperation(true)
                        .transactionStatus(TransactionStatus.FAILED)
                        .build();
                transactionRepository5.save(transaction);
                throw new Exception("Card is invalid");
            }

        }else{
            Transaction transaction = Transaction.builder()
                    .transactionDate(new Date())
                    .isIssueOperation(true)
                    .transactionStatus(TransactionStatus.FAILED)
                    .build();
            transactionRepository5.save(transaction);
            throw new Exception("Book is either unavailable or not present");
        }


        //Note that the error message should match exactly in all cases

       return transactionId; //return transactionId instead
    }

    public Transaction returnBook(int cardId, int bookId) throws Exception{

        List<Transaction> transactions = transactionRepository5.find(cardId, bookId,TransactionStatus.SUCCESSFUL, true);
        Transaction transaction = transactions.get(transactions.size() - 1);

        //for the given transaction calculate the fine amount considering the book has been returned exactly when this function is called
        //make the book available for other users
        //make a new transaction for return book which contains the fine amount as well

        Book book = bookRepository5.findById(bookId).get();
        Card card = cardRepository5.findById(cardId).get();


        Transaction returnBookTransaction  = Transaction.builder()
                .book(book)
                .card(card)
                .isIssueOperation(true)
                .transactionDate(new Date())
                .transactionStatus(TransactionStatus.SUCCESSFUL)
                .build();

        List<Transaction> transactionsList = book.getTransactions();
        transactionsList.add(returnBookTransaction);
        book.setTransactions(transactionsList);
        bookRepository5.updateBook(book);
        transactionRepository5.save(transaction);

        return returnBookTransaction; //return the transaction after updating all details
    }
}