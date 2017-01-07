package ru.javabegin.training.web.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import ru.javabegin.training.web.enums.SearchType;

@ManagedBean
@RequestScoped
public class SearchTypeController {

    private static Map<String, SearchType> searchList = new HashMap<String, SearchType>(); // хранит все виды поисков (по автору, по названию)

    public SearchTypeController() {

        ResourceBundle bundle = ResourceBundle.getBundle("ru.javabegin.training.web.nls.messages", FacesContext.getCurrentInstance().getViewRoot().getLocale());
        searchList.clear();
        searchList.put(bundle.getString("author_name"), SearchType.AUTHOR);
        searchList.put(bundle.getString("book_name"), SearchType.TITLE);
    }

    public Map<String, SearchType> getSearchList() {
        return searchList;
    }
}
