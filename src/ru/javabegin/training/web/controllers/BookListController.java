package ru.javabegin.training.web.controllers;

import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
//import java.util.HashMap;
import java.util.Map;
//import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;

import ru.javabegin.training.web.beans.Book;
import ru.javabegin.training.web.db.Database;
import ru.javabegin.training.web.enums.SearchType;

@ManagedBean(eager = true)
@SessionScoped
public class BookListController implements Serializable {

    private boolean requestFromPager;
    private int booksOnPage = 2;
    private int pageCount;
    private int selectedGenreId; // выбранный жанр
    private char selectedLetter; // выбранная буква алфавита
    private long selectedPageNumber = 1; // выбранный номер страницы в постраничной навигации
    private long totalBooksCount; // общее кол-во книг (не на текущей странице, а всего), нажно для постраничности
    private ArrayList<Integer> pageNumbers = new ArrayList<Integer>(); // общее кол-во книг (не на текущей странице, а всего), нажно для постраничности
    private SearchType searchType;// хранит выбранный тип поиска
    private String searchString; // хранит поисковую строку
   // private Map<String, SearchType> searchList = new HashMap<String, SearchType>(); // хранит все виды поисков (по автору, по названию)
    private ArrayList<Book> currentBookList; // текущий список книг для отображения
    private String currentSql;// последний выполнный sql без добавления limit

    public BookListController() {
        fillBooksAll();

     //   ResourceBundle bundle = ResourceBundle.getBundle("ru.javabegin.training.web.nls.messages", FacesContext.getCurrentInstance().getViewRoot().getLocale());
       // searchList.put(bundle.getString("author_name"), SearchType.AUTHOR);
        //searchList.put(bundle.getString("book_name"), SearchType.TITLE);

    }

    private void fillBooksBySQL(String sql) {

        StringBuilder sqlBuilder = new StringBuilder(sql);

        currentSql = sql;

        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;

        try {
            conn = Database.getConnection();
            stmt = conn.createStatement();

            System.out.println(requestFromPager);
            if (!requestFromPager) {

                String temp = sqlBuilder.toString();

                rs = stmt.executeQuery(temp);

                rs.last();

                totalBooksCount = rs.getRow();
                fillPageNumbers(totalBooksCount, booksOnPage);

            }



            if (totalBooksCount > booksOnPage) {
                sqlBuilder.append(" limit ").append(selectedPageNumber * booksOnPage - booksOnPage).append(",").append(booksOnPage);
            }

            rs = stmt.executeQuery(sqlBuilder.toString());

            currentBookList = new ArrayList<Book>();

            while (rs.next()) {
                Book book = new Book();
                book.setId(rs.getLong("book.id"));
                book.setName(rs.getString("book.name"));
                book.setGenre(rs.getString("book.genre_id"));
                book.setIsbn(rs.getString("book.isbn"));
                book.setAuthor(rs.getString("author.fio"));
                book.setPageCount(rs.getInt("book.page_count"));
                book.setPublishDate(rs.getInt("book.publish_year"));
                book.setPublisher(rs.getString("publisher.name"));
//              book.setImage(rs.getBytes("image"));
//              book.setContent(rs.getBytes("content"));
                book.setDescription(rs.getString("book.description"));
                currentBookList.add(book);
            }

        } catch (SQLException ex) {
            Logger.getLogger(BookListController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(BookListController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private void fillBooksAll() {
        fillBooksBySQL("select * from library.book "
                + "inner join library.author on "
                + "library.book.author_id=library.author.id "
                + "inner join library.publisher on "
                + "library.publisher.id=library.book.publisher_id");
    }

    private void submitValues(Character selectedLetter, long selectedPageNumber, int selectedGenreId, boolean requestFromPager) {
        this.selectedLetter = selectedLetter;
        this.selectedPageNumber = selectedPageNumber;
        this.selectedGenreId = selectedGenreId;
        this.requestFromPager = requestFromPager;

    }

    public String fillBooksByGenre() {

        imitateLoading();

        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        submitValues(' ', 1, Integer.valueOf(params.get("genre_id")), false);

        fillBooksBySQL("select * from library.book "
                + "inner join library.author on "
                + "library.book.author_id=library.author.id "
                + "inner join library.publisher on "
                + "library.publisher.id=library.book.publisher_id"
                + " where genre_id=" + selectedGenreId +"");



        return "books";
    }

    public String fillBooksByLetter() {

        imitateLoading();

        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        selectedLetter = params.get("letter").charAt(0);

        submitValues(selectedLetter, 1, -1, false);

        fillBooksBySQL("select * from library.book inner join library.author on "
                + " library.book.author_id=library.author.id "
                + " inner join library.publisher on  "
                + " library.publisher.id=library.book.publisher_id  "
                + " where lcase(left(library.book.name,1))='" + selectedLetter + "' ");

        return "books";
    }

    public String fillBooksBySearch() {

        imitateLoading();

        submitValues(' ', 1, -1, false);

        if (searchString.trim().length() == 0) {
            fillBooksAll();
            return "books";
        }

        StringBuilder sql = new StringBuilder("select * from library.book "
                + "inner join library.author on library.book.author_id=library.author.id "
                + "inner join library.genre on library.book.genre_id=library.genre.id "
                + "inner join library.publisher on library.book.publisher_id=library.publisher.id ");

        if (searchType == SearchType.AUTHOR) {
            sql.append("where lower(library.author.fio) like '%" + searchString.toLowerCase() + "%' order by library.book.name ");

        } else if (searchType == SearchType.TITLE) {
            sql.append("where lower(library.book.name) like '%" + searchString.toLowerCase() + "%' order by library.book.name ");
        }



        fillBooksBySQL(sql.toString());


        return "books";
    }

    public void selectPage() {
        cancelEdit();
        imitateLoading();
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        selectedPageNumber = Integer.valueOf(params.get("page_number"));
        requestFromPager = true;
        fillBooksBySQL(currentSql);
    }

    public byte[] getContent(int id) {
        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;


        byte[] content = null;
        try {
            conn = Database.getConnection();
            stmt = conn.createStatement();

            rs = stmt.executeQuery("select content from library.book where id=" + id);
            while (rs.next()) {
                content = rs.getBytes("content");
            }
        } catch (SQLException ex) {
            Logger.getLogger(Book.class
                    .getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(Book.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }

        return content;

    }

    public byte[] getImage(int id) {
        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;

        byte[] image = null;

        try {
            conn = Database.getConnection();
            stmt = conn.createStatement();

            rs = stmt.executeQuery("select image from library.book where id=" + id);
            while (rs.next()) {
                image = rs.getBytes("image");
            }

        } catch (SQLException ex) {
            Logger.getLogger(Book.class
                    .getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(Book.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }

        return image;
    }

    public String updateBooks() {
        imitateLoading();

        PreparedStatement prepStmt = null;
            ResultSet rs = null;
        Connection conn = null;

        try {
            conn = Database.getConnection();
            prepStmt = conn.prepareStatement("update library.book set name=?, isbn=?, page_count=?, publish_year=?, description=? where id=?");


            for (Book book : currentBookList) {
                if (!book.isEdit()) continue;
                prepStmt.setString(1, book.getName());
                prepStmt.setString(2, book.getIsbn());
//                prepStmt.setString(3, book.getAuthor());
                prepStmt.setInt(3, book.getPageCount());
                prepStmt.setInt(4, book.getPublishDate());
//                prepStmt.setString(6, book.getPublisher());
                prepStmt.setString(5, book.getDescription());
                prepStmt.setLong(6, book.getId());
                prepStmt.addBatch();
            }


            prepStmt.executeBatch();


        } catch (SQLException ex) {
            Logger.getLogger(BookListController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (prepStmt != null) {
                    prepStmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(BookListController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        cancelEdit();
        return "books";
    }
    private boolean editMode;

    public boolean isEditMode() {
        return editMode;
    }

    public void showEdit() {
        editMode = true;
    }

    public void cancelEdit(){
        editMode = false;
        for (Book book : currentBookList) {
            book.setEdit(false);
        }
    }


public Character[] getRussianLetters() {
        Character[] letters = new Character[35];
        letters[0] = 'А';
        letters[1] = 'Б';
        letters[2] = 'В';
        letters[3] = 'Г';
        letters[4] = 'Д';
        letters[5] = 'Е';
        letters[6] = 'Ё';
        letters[7] = 'Ж';
        letters[8] = 'З';
        letters[9] = 'И';
        letters[10] = 'Й';
        letters[11] = 'К';
        letters[12] = 'Л';
        letters[13] = 'М';
        letters[14] = 'Н';
        letters[15] = 'О';
        letters[16] = 'П';
        letters[17] = 'Р';
        letters[18] = 'С';
        letters[19] = 'Т';
        letters[20] = 'У';
        letters[21] = 'Ф';
        letters[22] = 'Х';
        letters[23] = 'Ц';
        letters[24] = 'Ч';
        letters[25] = 'Ш';
        letters[26] = 'Щ';
        letters[27] = 'Ъ';
        letters[28] = 'Ы';
        letters[29] = 'Ь';
        letters[30] = 'Э';
        letters[31] = 'Ю';
        letters[32] = 'Я';
        letters[33] = 'U';
        letters[34] = 'W';

        return letters;
    }

    public void booksOnPageChanged(ValueChangeEvent e) {
        imitateLoading();
        cancelEdit();
        requestFromPager = false;
        booksOnPage = Integer.valueOf(e.getNewValue().toString()).intValue();
        selectedPageNumber = 1;
        fillBooksBySQL(currentSql);
    }

    private void fillPageNumbers(long totalBooksCount, int booksCountOnPage) {

        if (totalBooksCount % booksCountOnPage == 0) {
            pageCount = booksCountOnPage > 0 ? (int) (totalBooksCount / booksCountOnPage) : 0;
        } else {
            pageCount = booksCountOnPage > 0 ? (int) (totalBooksCount / booksCountOnPage) + 1 : 0;
        }

        pageNumbers.clear();
        for (int i = 1; i <= pageCount; i++) {
            pageNumbers.add(i);
        }

    }

    private void imitateLoading() {
        try {
            Thread.sleep(1000);// имитация загрузки процесса
        } catch (InterruptedException ex) {
            Logger.getLogger(BookListController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void searchStringChanged(ValueChangeEvent e) {
        searchString = e.getNewValue().toString();
    }

    public void searchTypeChanged(ValueChangeEvent e) {
        searchType = (SearchType) e.getNewValue();
    }


    public ArrayList<Integer> getPageNumbers() {
        return pageNumbers;
    }

    public void setPageNumbers(ArrayList<Integer> pageNumbers) {
        this.pageNumbers = pageNumbers;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public SearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(SearchType searchType) {
        this.searchType = searchType;
    }

  //  public Map<String, SearchType> getSearchList() {
 //       return searchList;
  //  }

    public ArrayList<Book> getCurrentBookList() {
        return currentBookList;
    }

    public void setTotalBooksCount(long booksCount) {
        this.totalBooksCount = booksCount;
    }

    public long getTotalBooksCount() {
        return totalBooksCount;
    }

    public int getSelectedGenreId() {
        return selectedGenreId;
    }

    public void setSelectedGenreId(int selectedGenreId) {
        this.selectedGenreId = selectedGenreId;
    }

    public char getSelectedLetter() {
        return selectedLetter;
    }

    public void setSelectedLetter(char selectedLetter) {
        this.selectedLetter = selectedLetter;
    }

    public int getBooksOnPage() {
        return booksOnPage;
    }

    public void setBooksOnPage(int booksOnPage) {
        this.booksOnPage = booksOnPage;
    }

    public void setSelectedPageNumber(long selectedPageNumber) {
        this.selectedPageNumber = selectedPageNumber;
    }

    public long getSelectedPageNumber() {
        return selectedPageNumber;
    }
}
