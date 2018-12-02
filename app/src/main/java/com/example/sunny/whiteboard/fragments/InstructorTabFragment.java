package com.example.sunny.whiteboard.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.sunny.whiteboard.MainActivity;
import com.example.sunny.whiteboard.MessagesActivity;
import com.example.sunny.whiteboard.R;
import com.example.sunny.whiteboard.TabActivity;
import com.example.sunny.whiteboard.adapters.MessageAdapter;
import com.example.sunny.whiteboard.models.Message;
import com.example.sunny.whiteboard.models.Project;
import com.example.sunny.whiteboard.models.User;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class InstructorTabFragment extends Fragment {
    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private EditText edtEditMessage;
    private Button btnSend;

    private String chatType;
    private String projectID;
    private Project project;

    private CollectionReference currentChat;
    private FirebaseFirestore db;
    private User user;

    private static final String TAG = "InstructorTabFragment";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_chat, container, false);

        // get chatType from current tab - group or instructor
        project = TabActivity.project;
        chatType = "instructor";

        // initialize firebase backend
        db = FirebaseFirestore.getInstance();
        user = MainActivity.user;
        currentChat = db.collection("messages").document(project.getID()).collection(chatType);

        // set views
        recyclerView = view.findViewById(R.id.fragment_group_chat_recycler_view);
        edtEditMessage = view.findViewById(R.id.fragment_group_chat_edt_message);
        btnSend = view.findViewById(R.id.fragment_group_chat_btn_send);

        // use if possible - improves performance
        //recyclerView.setHasFixedSize(10);

        // create a layout manager for the recycler view
        layoutManager = new LinearLayoutManager(view.getContext());
        recyclerView.setLayoutManager(layoutManager);
        listenForMessage();

        // send message
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        return view;
    }

    // handles message retrieval from firebase
    private void listenForMessage() {
        currentChat.orderBy("timestamp").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                // retrieve message list and display on screen
                ArrayList<Message> messages =
                        Message.convertFirebaseMessages(queryDocumentSnapshots.getDocuments());
                messageAdapter = new MessageAdapter(messages);
                recyclerView.setAdapter(messageAdapter);
            }
        });
    }

    // handles message sending
    private void sendMessage() {

        /*
        ***********
        * Need to add instructors to list of members
        * append instructor emails to project member list
        ***********
         */


        String text = edtEditMessage.getText().toString();
        if (!text.equals("")) {
            currentChat.add(new Message(currentChat.getId(), text, user.getUID(), project.getMembers(),
                    System.currentTimeMillis() / 1000));
            recyclerView.scrollToPosition(messageAdapter.getItemCount());
            edtEditMessage.setText("");
            //hideKeyboard();
        }
        else
            Toast.makeText(getContext(), "Please enter a message", Toast.LENGTH_SHORT).show();

    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

}
