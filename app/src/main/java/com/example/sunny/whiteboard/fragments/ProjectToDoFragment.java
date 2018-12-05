package com.example.sunny.whiteboard.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

import android.widget.Toast;


import com.example.sunny.whiteboard.R;
import com.example.sunny.whiteboard.TabActivity;
import com.example.sunny.whiteboard.models.Project;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.sunny.whiteboard.TabActivity.project;

public class ProjectToDoFragment extends Fragment {
    FirebaseFirestore db;
    @BindView(R.id.btn)
    Button button;
    @BindView(R.id.edittext)
    EditText editText;
    @BindView(R.id.listView)
    ListView listView;
    private List<String> tasksList = new ArrayList<>();
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {

        View view = inflater.inflate(R.layout.fragment_todo, container, false);

        ButterKnife.bind(this, view);
        db = FirebaseFirestore.getInstance();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String content;
                content = editText.getText().toString();
                savelist(content);

            }
        });

        db.collection("projects").document(project.getID()).collection("todo").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                tasksList.clear();
                for (DocumentSnapshot snapshot : documentSnapshots) {
                    tasksList.add(snapshot.getString("todolist"));
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_selectable_list_item, tasksList);
                adapter.notifyDataSetChanged();
                listView.setAdapter(adapter);
            }
        });

        return view;

    }
    public void savelist  (String arg1)
    {    Map<String, Object> savetodo  = new HashMap<>();

        savetodo.put("todolist",arg1);

        db.collection("projects").document(project.getID()).collection("todo").add(savetodo).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {

            }
        });
    }

}




