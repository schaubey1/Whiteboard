package com.example.sunny.whiteboard.models;

import android.content.Context;
import android.content.SharedPreferences;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class User {
    private String uid;
    private String name;
    private String email;
    private String accountType;
    private ArrayList<String> projectList;

    public User(String uid, String name, String email, String accountType, ArrayList<String> projectList) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.accountType = accountType;
        this.projectList = projectList;
    }

    public User() { }

    public String getUID() { return uid; }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getAccountType() { return accountType; }

    public ArrayList<String> getProjectList() { return projectList; }

    // write user to SharedPreferences
    public static void writeUser(Context context, User user) {
        SharedPreferences sh = context.getSharedPreferences("Whiteboard", Context.MODE_PRIVATE);
        SharedPreferences.Editor shEdit = sh.edit();
        shEdit.putString("uid", user.getUID());
        shEdit.putString("name", user.getName());
        shEdit.putString("email", user.getEmail());
        shEdit.putString("accountType", user.getAccountType());
        shEdit.commit();
    }

    // retrieve user from SharedPreferences
    public static User getUser(Context context) {
        SharedPreferences sh = context.getSharedPreferences("Whiteboard", Context.MODE_PRIVATE);
        String uid = sh.getString("uid", "");
        String name = sh.getString("name", "");
        String email = sh.getString("email", "");
        String accountType = sh.getString("accountType", "");
        return new User(uid, name, email, accountType, new ArrayList<String>());
    }

    // delete user's SharedPreferences
    public static void deleteUser(Context context) {
        SharedPreferences sh = context.getSharedPreferences("Whiteboard", Context.MODE_PRIVATE);
        SharedPreferences.Editor shEdit = sh.edit();
        shEdit.clear();
        shEdit.commit();
    }

    public static ArrayList<User> convertEmailToUsers(ArrayList<String> emails) {
        ArrayList<User> users = new ArrayList<>();
        for (String email : emails) {
            users.add(new User(null, null, email, null, null));
        }
        return users;
    }
}
