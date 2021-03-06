package com.example.sunny.whiteboard.classes;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.sunny.whiteboard.R;
import com.example.sunny.whiteboard.adapters.ClassAdapter;
import com.example.sunny.whiteboard.messages.MessagesActivity;
import com.example.sunny.whiteboard.models.Class;
import com.example.sunny.whiteboard.models.Project;
import com.example.sunny.whiteboard.models.User;
import com.example.sunny.whiteboard.projects.ProjectApprovalActivity;
import com.example.sunny.whiteboard.projects.ProjectsActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.reflect.ClassPath;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class ClassesActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ClassAdapter.OnItemClickListener {

    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private FloatingActionButton fabJoinClassStudent;
    private com.getbase.floatingactionbutton.FloatingActionsMenu fabMenu;
    private com.getbase.floatingactionbutton.FloatingActionButton fabCreateClass;
    private com.getbase.floatingactionbutton.FloatingActionButton fabJoinClass;

    private RecyclerView recyclerView;
    private ClassAdapter adapter;

    private FirebaseFirestore db;
    private User user;
    private String userType;

    public static final String CLASS_KEY = "class";
    private static final String TAG = "ClassActivityLog";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classes);

        // set views
        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        fabJoinClassStudent = findViewById(R.id.activity_classes_fab);
        fabMenu = findViewById(R.id.activity_classes_fab_menu);
        fabCreateClass = findViewById(R.id.fab_create_class);
        fabJoinClass = findViewById(R.id.fab_join_class);

        // set up firebase
        db = FirebaseFirestore.getInstance();
        user = ProjectsActivity.user;

        // get current account type for class filtering
        userType = ProjectsActivity.userType;

        // setup sidebar/navigation
        navigationView.setNavigationItemSelectedListener(this);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        setSupportActionBar(toolbar);

        // setup recycler view
        recyclerView = findViewById(R.id.activity_classes_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // retrieve classes for current user
        db.collection("classes").whereArrayContains(userType, user.getEmail())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }

                        // display class list to screen
                        ArrayList<Class> classes =
                                Class.convertFirebaseProjects(queryDocumentSnapshots.getDocuments());
                        if (classes != null)
                        displayClasses(classes);
                    }
                });

        // add a new class for student and instructor
        if (userType.equals("students")) {
            fabMenu.setVisibility(View.GONE);

            fabJoinClassStudent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ClassesActivity.this);
                    View view = getLayoutInflater().inflate(R.layout.dialog_join_class, null);
                    builder.setView(view);

                    // display dialog
                    final AlertDialog dialog = builder.create();
                    dialog.show();

                    final EditText edtCode = view.findViewById(R.id.Code);
                    Button enter = view.findViewById(R.id.Enter);

                    // attempt to join class with inputted code
                    enter.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final String code = edtCode.getText().toString();
                            if (!code.isEmpty()) {

                                // find class with matching class code
                                db.collection("classes").whereEqualTo("code", code)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                // add current user to class students list
                                                if (task.getResult().getDocuments().size() > 0) {
                                                    DocumentSnapshot selectedClass = task.getResult().getDocuments().get(0);
                                                    selectedClass.getReference()
                                                            .update(userType, FieldValue.arrayUnion(user.getEmail()));

                                                    // update all existing projects for a class with new instructor
                                                    if (userType.equals("instructors")) {
                                                        String className = selectedClass.getString("className");
                                                        db.collection("projects").whereEqualTo("className", className)
                                                                .get()
                                                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                        if (task.getResult() != null) {
                                                                            // add instructor's email to instructor list of all projects for joined class
                                                                            List<DocumentSnapshot> projects = task.getResult().getDocuments();
                                                                            for (DocumentSnapshot project : projects) {
                                                                                project.getReference().update("instructors", FieldValue.arrayUnion(user.getEmail()));
                                                                            }
                                                                        }
                                                                    }
                                                                });
                                                    }

                                                    Toast.makeText(ClassesActivity.this, "Class entry successful!", Toast.LENGTH_SHORT).show();
                                                    dialog.dismiss();
                                                } else {
                                                    Toast.makeText(getApplicationContext(),
                                                            "No class with this code found", Toast.LENGTH_SHORT).show();

                                                }
                                            }
                                        });
                            } else {
                                Toast.makeText(ClassesActivity.this, "Please enter a code", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }

            });
        } else {
            // handles class creation as instructor - returns a popup with the code for the new class
            fabJoinClassStudent.setVisibility(View.GONE);
            fabCreateClass.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fabMenu.collapse();

                    final AlertDialog.Builder builder = new AlertDialog.Builder(ClassesActivity.this);
                    View view = getLayoutInflater().inflate(R.layout.dialog_instructor_create_class, null);
                    builder.setView(view);

                    // display the dialog on-screen
                    final AlertDialog dialog = builder.create();
                    dialog.show();

                    final AlertDialog dialogCode = builder.create();

                    // set views
                    final EditText edtClassName = view.findViewById(R.id.ClassName);
                    Button btnCreate = view.findViewById(R.id.Create);

                    // make class entry, return popup with generated code
                    btnCreate.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(final View v) {
                            final String className = edtClassName.getText().toString();
                            if(!className.isEmpty()) {
                                db.collection("classes").whereEqualTo("className", className)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if (task.getResult() != null) {

                                                    // check if class has already been made
                                                    ArrayList<DocumentSnapshot> classes = (ArrayList<DocumentSnapshot>) task.getResult().getDocuments();
                                                    if (classes.size() < 1) {
                                                        ArrayList<String> instructors = new ArrayList<>();
                                                        instructors.add(user.getEmail());
                                                        final String code = Class.generateCode();
                                                        Class newClass = new Class(className, code, null, instructors, null);
                                                        db.collection("classes").add(newClass)
                                                                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<DocumentReference> task) {
                                                                DocumentReference newClass = task.getResult();
                                                                task.getResult().update("id", newClass.getId());

                                                                // close popup and show snackbar with code copy
                                                                dialog.dismiss();
                                                                copyCode(code);
                                                            }
                                                        });
                                                    } else {
                                                        Toast.makeText(getApplicationContext(), "This class already exists", Toast.LENGTH_SHORT).show();
                                                        edtClassName.setText("");
                                                        return;
                                                    }
                                                }
                                            }
                                        });
                            } else {
                                Toast.makeText(ClassesActivity.this, "Please fill in empty fields", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });

            // handles class joining as an instructor
            fabJoinClass.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fabMenu.collapse();
                    AlertDialog.Builder builder = new AlertDialog.Builder(ClassesActivity.this);
                    View view = getLayoutInflater().inflate(R.layout.dialog_join_class, null);
                    builder.setView(view);

                    // display dialog
                    final AlertDialog dialog = builder.create();
                    dialog.show();

                    final EditText edtCode = view.findViewById(R.id.Code);
                    Button enter = view.findViewById(R.id.Enter);

                    // attempt to join class with inputted code
                    enter.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final String code = edtCode.getText().toString();
                            if (!code.isEmpty()) {

                                // find class with matching class code
                                db.collection("classes").whereEqualTo("code", code)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                // add current user to class students list
                                                if (task.getResult().getDocuments().size() > 0) {
                                                    DocumentSnapshot selectedClass = task.getResult().getDocuments().get(0);
                                                    selectedClass.getReference()
                                                            .update(userType, FieldValue.arrayUnion(user.getEmail()));

                                                    // update all existing projects for a class with new instructor
                                                    if (userType.equals("instructors")) {
                                                        String className = selectedClass.getString("className");
                                                        db.collection("projects").whereEqualTo("className", className)
                                                                .get()
                                                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                        if (task.getResult() != null) {
                                                                            // add instructor's email to instructor list of all projects for joined class
                                                                            List<DocumentSnapshot> projects = task.getResult().getDocuments();
                                                                            for (DocumentSnapshot project : projects) {
                                                                                project.getReference().update("instructors", FieldValue.arrayUnion(user.getEmail()));
                                                                            }
                                                                        }
                                                                    }
                                                                });
                                                    }

                                                    Toast.makeText(ClassesActivity.this, "Class entry successful!", Toast.LENGTH_SHORT).show();
                                                    dialog.dismiss();
                                                } else {
                                                    Toast.makeText(getApplicationContext(),
                                                            "No class with this code found", Toast.LENGTH_SHORT).show();

                                                }
                                            }
                                        });
                            } else {
                                Toast.makeText(ClassesActivity.this, "Please enter a code", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });
        }

    }

    // handles user removal from a class
    private void leaveClass(final Class selectedClass) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ClassesActivity.this);
        View view = getLayoutInflater().inflate(R.layout.dialog_leave_class, null);
        builder.setView(view);

        // display dialog
        final AlertDialog dialog = builder.create();
        dialog.show();

        final Button btnLeave = view.findViewById(R.id.dialog_leave_class_btn_yes);
        final Button btnCancel = view.findViewById(R.id.dialog_leave_class_btn_no);

        // handles user removal from class
        btnLeave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                // remove user from class's students list
                final Map<String, Object> deleteUser = new HashMap<>();
                deleteUser.put(userType, FieldValue.arrayRemove(user.getEmail()));
                db.collection("classes").document(selectedClass.getID())
                        .update(deleteUser);

                // remove user from any project in that class(members)
                db.collection("projects")
                        .whereArrayContains(userType, user.getEmail())
                        .whereEqualTo("className", selectedClass.getClassName())
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.getResult() != null) {
                                    List<DocumentSnapshot> userProjects = task.getResult().getDocuments();

                                    // delete user from all projects enrolled in
                                    if (userProjects.size() > 0) {
                                        // get reference to each project and delete
                                        for (final DocumentSnapshot project : userProjects) {
                                            final DocumentReference projectRef = project.getReference();
                                            projectRef
                                                    .get()
                                                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                    if (task.getResult() != null) {
                                                        DocumentSnapshot project = task.getResult();
                                                        int numStudents = ((ArrayList<String>) project.get(userType)).size() - 1;
                                                        if (numStudents < 1) {
                                                            // no students left in project, delete it
                                                            projectRef.delete();
                                                        }
                                                        else
                                                            projectRef.update(deleteUser);

                                                        if (userType.equals("students")) {
                                                            // delete project from user's projectList
                                                            String projectName = project.getString("name");
                                                            final Map<String, Object> deleteProject = new HashMap<>();
                                                            deleteProject.put("projectList", FieldValue.arrayRemove(projectName));
                                                            db.collection("users")
                                                                    .document(user.getUID()).update(deleteProject);
                                                        }
                                                    }
                                                }
                                            });

                                        }
                                    }
                                }
                            }
                        });

                // update user's projectList

                dialog.dismiss();
                Toast.makeText(view.getContext(),
                        "Successfully removed the class!", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // dismiss popup
                dialog.dismiss();
            }
        });
    }

    // displays class enrollment code and lets user copy
    private void copyCode(final String code) {
        // show snackbar with enrollment copy option
        Snackbar barCode = Snackbar.make(drawer, "Class created. Copy class enrollment code", Snackbar.LENGTH_INDEFINITE)
                .setAction("COPY", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("Class enrollment code", code);
                        clipboard.setPrimaryClip(clip);
                    }
                });
        barCode.show();
    }

    // handles recycler view building to display classes
    private void displayClasses(ArrayList<Class> classes) {
        adapter = new ClassAdapter(classes);
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(this);
    }

    // handles click event for class elements
    public void onItemClick(Class currClass) {
        Intent intent = new Intent(getApplicationContext(), ClassInfoActivity.class);
        intent.putExtra(CLASS_KEY, currClass);
        startActivity(intent);
    }

    // displays popup to leave class on class long press
    @Override
    public void onLongClick(Class currClass) {
        leaveClass(currClass);
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.side_bar, menu);
        return true;
    }

    // Handle action bar item clicks here.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawer.openDrawer(GravityCompat.START);
                break;

            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_classes:
                // Handle the classes action
                Intent i = new Intent(this, ClassesActivity.class);
                startActivity(i);
                break;
            case R.id.nav_projmanagement:
                if (user.getAccountType().equals("student"))
                    startActivity(new Intent(this, ProjectsActivity.class));
                else
                    startActivity(new Intent(this, ProjectApprovalActivity.class));
                break;
            case R.id.nav_messages:
                // Handle the project management action
                Intent l = new Intent(this, MessagesActivity.class);
                startActivity(l);
                break;
            case R.id.nav_sign_out:
                // handle user sign out
                ProjectsActivity.signOut(this);
                break;
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}