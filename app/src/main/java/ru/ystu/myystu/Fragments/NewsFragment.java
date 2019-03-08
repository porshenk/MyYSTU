package ru.ystu.myystu.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import ru.ystu.myystu.DataFragments.DataFragment_News_List;
import ru.ystu.myystu.R;
import ru.ystu.myystu.Adapters.NewsItemsAdapter;
import ru.ystu.myystu.AdaptersData.NewsItemsData_Header;
import ru.ystu.myystu.Network.GetListNewsFromURL;

public class NewsFragment extends Fragment {

    private int PHOTO_SIZE = 100;                                                                   // Качество загружаемых картинок (50, 100, 200)
    private int OFFSET = 0;                                                                         // Смещение для следующей порции новостей (не менять)
    private int POST_COUNT_LOAD = 20;                                                               // Количество загружаемых постов за раз
    private String OWNER_ID = "-28414014";                                                          // id группы вуза через дефис
    //private String OWNER_ID = "-178529732";                                                       // id группы тестовой
    private String VK_API_VERSION = "5.92";                                                         // Версия API
    private String SERVICE_KEY
            = "7c2b4e597c2b4e597c2b4e59ef7c43691577c2b7c2b4e5920683355158fece460f119b9";            // Сервисный ключ доступа

    private int postionScroll = 0;
    private boolean isLoad = false;
    private boolean isEnd = false;

    private StringBuilder urlBuilder = new StringBuilder();

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mRecyclerViewAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private ArrayList<Parcelable> mList;
    private Parcelable mRecyclerState;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private CompositeDisposable mDisposables;
    private GetListNewsFromURL getListNewsFromURL;

    private DataFragment_News_List dataFragment_news_list;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(false);

        mDisposables = new CompositeDisposable();
        getListNewsFromURL = new GetListNewsFromURL();
        final FragmentManager mFragmentManager = getFragmentManager();

        if (mFragmentManager != null) {
            dataFragment_news_list = (DataFragment_News_List) mFragmentManager.findFragmentByTag("news_list");

            if (dataFragment_news_list == null) {
                dataFragment_news_list = new DataFragment_News_List();
                mFragmentManager.beginTransaction().add(dataFragment_news_list, "news_list").commit();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        final ImagePipeline mImagePipeline = Fresco.getImagePipeline();
        mDisposables.dispose();
        mImagePipeline.clearMemoryCaches();
    }

    @Override
    public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mList.add(new NewsItemsData_Header(0, "Тестирую header"));

        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent,
                R.color.colorPrimary);

        if(savedInstanceState == null){
            getNews(false);
        } else {
            mList = dataFragment_news_list.getList();
            mRecyclerState = savedInstanceState.getParcelable("recyclerViewState");
            mLayoutManager.onRestoreInstanceState(mRecyclerState);
            mRecyclerViewAdapter = new NewsItemsAdapter(mList, getContext());
            mRecyclerView.setAdapter(mRecyclerViewAdapter);
            postionScroll = savedInstanceState.getInt("postionScroll");
            OFFSET = savedInstanceState.getInt("offset");
        }

        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            getNews(false);
        });

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull final RecyclerView recyclerView, final int dx, final int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // Прокрутили список до конца (5 элемент с конца)
                if( ((LinearLayoutManager)mLayoutManager).findFirstVisibleItemPosition()
                        >= mLayoutManager.getItemCount() - 5
                        && mLayoutManager.getItemCount() > 0
                        && !isLoad
                        && !isEnd){
                    getNews(true);
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);

        mRecyclerState = mLayoutManager.onSaveInstanceState();
        outState.putParcelable("recyclerViewState", mRecyclerState);
        outState.putInt("postionScroll", postionScroll);
        outState.putInt("offset", OFFSET);

        dataFragment_news_list.setList(mList);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        final View mView = inflater.inflate(R.layout.fragment_news, container, false);

        if(mView != null){
            mRecyclerView = mView.findViewById(R.id.recycler_news_items);
            mSwipeRefreshLayout = mView.findViewById(R.id.refresh_news);
        }

        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mList = new ArrayList<>();

        return mView;
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void scrollTopRecyclerView() {

        if(mRecyclerView != null){
            if(((LinearLayoutManager)mLayoutManager).findFirstVisibleItemPosition() > 0){
                postionScroll = ((LinearLayoutManager)mLayoutManager).findFirstCompletelyVisibleItemPosition();
                if(((LinearLayoutManager)mLayoutManager).findFirstVisibleItemPosition() < 10)
                    mRecyclerView.smoothScrollToPosition(0);
                else{
                    mRecyclerView.scrollToPosition(5);
                    mRecyclerView.smoothScrollToPosition(0);
                }
            } else{
                if(postionScroll > 0){
                    mRecyclerView.scrollToPosition(postionScroll - 1);
                    mRecyclerView.smoothScrollToPosition(postionScroll);
                }
            }
        }
    }

    // Запрос к API
    private String getUrl(final boolean isOffset){

        if(isOffset)
            OFFSET += POST_COUNT_LOAD + 1;
        else
            OFFSET = 0;

        if(urlBuilder.length() > 0)
            urlBuilder.setLength(0);

        urlBuilder
                .append("https://api.vk.com/method/wall.get?owner_id=")
                .append(OWNER_ID)
                .append("&count=")
                .append(POST_COUNT_LOAD)
                .append("&filter=owner")
                .append("&offset=")
                .append(OFFSET)
                .append("&access_token=")
                .append(SERVICE_KEY)
                .append("&version=")
                .append(VK_API_VERSION);

        return urlBuilder.toString();
    }

    private void getNews(final boolean isOffset){

        final String url = getUrl(isOffset);
        int listCount = mList.size();

        if(!isLoad) {
            isLoad = true;
            mSwipeRefreshLayout.setRefreshing(true);

            final Observable<ArrayList<Parcelable>> observableNewsList
                    = getListNewsFromURL.getObservableNewsList(url, isOffset, mList);
            mDisposables.add(observableNewsList
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableObserver<ArrayList<Parcelable>>(){
                @Override
                public void onNext(ArrayList<Parcelable> parcelables) {
                    mList = parcelables;
                }

                @Override
                public void onError(Throwable e) {

                    try {

                        if(mRecyclerViewAdapter == null){
                            mRecyclerViewAdapter = new NewsItemsAdapter(mList, getContext());
                            mRecyclerViewAdapter.setHasStableIds(true);
                            mRecyclerView.setAdapter(mRecyclerViewAdapter);
                        }

                        isLoad = false;

                        if (mSwipeRefreshLayout.isRefreshing())
                            mSwipeRefreshLayout.setRefreshing(false);

                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

                    } finally {
                        dispose();
                    }

                }

                @Override
                public void onComplete() {
                    try {

                        isLoad = false;

                        if (isOffset) {
                            mRecyclerViewAdapter.notifyItemRangeInserted(listCount,
                                    mList.size() - listCount);
                        } else {
                            mRecyclerViewAdapter = new NewsItemsAdapter(mList, getContext());
                            mRecyclerViewAdapter.setHasStableIds(true);
                            mRecyclerView.setAdapter(mRecyclerViewAdapter);
                        }

                        if (mSwipeRefreshLayout.isRefreshing())
                            mSwipeRefreshLayout.setRefreshing(false);

                        // Конец списка новостей
                        isEnd = mList.size() <= listCount;

                    } finally {
                        dispose();
                    }
                }
            }));

        }
    }
}