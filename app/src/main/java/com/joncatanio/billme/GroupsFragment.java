package com.joncatanio.billme;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.joncatanio.billme.model.GroupShort;

import java.util.ArrayList;
import java.util.List;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GroupsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GroupsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GroupsFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private GroupObserver groupObserver;
    private GroupAdapter groupAdapter;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public GroupsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment GroupsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static GroupsFragment newInstance(String param1, String param2) {
        GroupsFragment fragment = new GroupsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_groups, container, false);
        fetchContent(rootView);

        return rootView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private void fetchContent(View rootView) {
        String authToken = BillMeApi.getAuthToken(getActivity());
        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.groups_recycler_view);
        assert recyclerView != null;
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        //billObserver = (BillObserver) getActivity().getLastNonConfigurationInstance();

        if (groupObserver == null) {
            groupObserver = new GroupObserver();
            groupAdapter = new GroupAdapter(groupObserver.getGroups());
            groupObserver.bind(getActivity(), this);

            BillMeApi.get()
                    .getGroups(authToken)
                    .flatMap(new Func1<List<GroupShort>, Observable<GroupShort>>() {
                        @Override
                        public Observable<GroupShort> call(List<GroupShort> groups) {
                            return Observable.from(groups);
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(groupObserver);
        } else {
            groupAdapter = new GroupAdapter(groupObserver.getGroups());
            groupObserver.bind(getActivity(), this);
        }

        recyclerView.setAdapter(groupAdapter);
    }

    private static class GroupObserver implements Observer<GroupShort> {
        private FragmentActivity mActivity;
        private GroupsFragment frag;

        private ArrayList<GroupShort> groups = new ArrayList<>();

        private void bind(FragmentActivity activity, GroupsFragment frag) {
            this.mActivity = activity;
            this.frag = frag;
        }

        private void unbind() {
            mActivity = null;
        }

        public ArrayList<GroupShort> getGroups() {
            return groups;
        }

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
            HttpException httpErr = (HttpException) e;

            if (httpErr.code() == 403) {
                // The user has an invalid/expired token, make them log in.
                Intent intent = new Intent(mActivity.getApplicationContext(), LoginActivity.class);
                mActivity.startActivity(intent);
            } else {
                Log.e("BillObserver", e.getMessage());
            }
        }

        @Override
        public void onNext(GroupShort bill) {
            int index = groups.size();
            groups.add(bill);
            frag.groupAdapter.notifyItemInserted(index);
        }
    }
}
