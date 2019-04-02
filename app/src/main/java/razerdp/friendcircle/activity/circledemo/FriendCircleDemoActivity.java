package razerdp.friendcircle.activity.circledemo;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.razerdp.github.com.common.MomentsType;
import com.razerdp.github.com.common.entity.CommentInfo;
import com.razerdp.github.com.common.entity.LikesInfo;
import com.razerdp.github.com.common.entity.MomentsInfo;
import com.razerdp.github.com.common.entity.UserInfo;
import com.razerdp.github.com.common.entity.other.ServiceInfo;
import com.razerdp.github.com.common.manager.LocalHostManager;
import com.razerdp.github.com.common.request.MomentsRequest;
import com.razerdp.github.com.common.router.RouterList;
import com.socks.library.KLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import cn.bmob.v3.exception.BmobException;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import razerdp.basepopup.BasePopupWindow;
import razerdp.friendcircle.R;
import razerdp.friendcircle.activity.ActivityLauncher;
import razerdp.friendcircle.app.manager.ServiceInfoManager;
import razerdp.friendcircle.app.manager.UpdateInfoManager;
import razerdp.friendcircle.app.mvp.model.UpdateInfo;
import razerdp.friendcircle.app.mvp.presenter.impl.MomentPresenter;
import razerdp.friendcircle.app.mvp.view.IMomentView;
import razerdp.friendcircle.ui.adapter.CircleMomentsAdapter;
import razerdp.friendcircle.ui.helper.TitleBarAlphaChangeHelper;
import razerdp.friendcircle.ui.viewholder.EmptyMomentsVH;
import razerdp.friendcircle.ui.viewholder.MultiImageMomentsVH;
import razerdp.friendcircle.ui.viewholder.TextOnlyMomentsVH;
import razerdp.friendcircle.ui.viewholder.WebMomentsVH;
import razerdp.friendcircle.ui.widget.popup.PopupTextAction;
import razerdp.friendcircle.ui.widget.popup.PopupUpdate;
import razerdp.friendcircle.ui.widget.popup.RegisterPopup;
import razerdp.github.com.lib.common.entity.ImageInfo;
import razerdp.github.com.lib.helper.AppFileHelper;
import razerdp.github.com.lib.helper.AppSetting;
import razerdp.github.com.lib.interfaces.SimpleCallback;
import razerdp.github.com.lib.interfaces.SingleClickListener;
import razerdp.github.com.lib.manager.KeyboardControlMnanager;
import razerdp.github.com.lib.network.base.OnResponseListener;
import razerdp.github.com.lib.utils.ToolUtil;
import razerdp.github.com.ui.base.BaseTitleBarActivity;
import razerdp.github.com.ui.helper.PhotoHelper;
import razerdp.github.com.ui.imageloader.ImageLoadMnanger;
import razerdp.github.com.ui.util.AnimUtils;
import razerdp.github.com.ui.util.UIHelper;
import razerdp.github.com.ui.widget.commentwidget.CommentBox;
import razerdp.github.com.ui.widget.commentwidget.CommentWidget;
import razerdp.github.com.ui.widget.commentwidget.IComment;
import razerdp.github.com.ui.widget.common.TitleBar;
import razerdp.github.com.ui.widget.popup.SelectPhotoMenuPopup;
import razerdp.github.com.ui.widget.pullrecyclerview.CircleRecyclerView;
import razerdp.github.com.ui.widget.pullrecyclerview.CircleRecyclerView.OnPreDispatchTouchListener;
import razerdp.github.com.ui.widget.pullrecyclerview.interfaces.OnRefreshListener2;

/**
 * Created by 大灯泡 on 2016/10/26.
 * <p>
 * 朋友圈主界面
 */

public class FriendCircleDemoActivity extends BaseTitleBarActivity implements OnRefreshListener2, IMomentView, OnPreDispatchTouchListener {

    private static final int REQUEST_REFRESH = 0x10;
    private static final int REQUEST_LOADMORE = 0x11;


    private int clickServiceCount = 0;
    private View mTipsLayout;
    private TextView mServiceTipsView;
    private ImageView mCloseImageView;

    private TextView mUpdateTipsView;
    private View mUpdateLayout;

    private CircleRecyclerView circleRecyclerView;
    private CommentBox commentBox;
    private HostViewHolder hostViewHolder;
    private CircleMomentsAdapter adapter;
    private List<MomentsInfo> momentsInfoList;
    //request
    private MomentsRequest momentsRequest;
    private MomentPresenter presenter;

    private CircleViewHelper mViewHelper;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        momentsInfoList = new ArrayList<>();
        momentsRequest = new MomentsRequest();
        initView();
        initKeyboardHeightObserver();
        UIHelper.ToastMessage("请尽量不要上传黄图，谢谢");

        UpdateInfoManager.INSTANCE.init(this, new BasePopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                delayCheckServiceInfo();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppFileHelper.initStroagePath(this);
    }

    @Override
    public void onHandleIntent(Intent intent) {

    }


    private void initView() {
        if (mViewHelper == null) {
            mViewHelper = new CircleViewHelper(this);
        }
        getTitleBar().getLeftTextView().setAlpha(0f);
        getTitleBar().setLeftText("朋友圈");
        setLeftTextColor(Color.parseColor("#040404"));
        setTitleMode(TitleBar.TitleBarMode.MODE_BOTH);
        setTitleRightIcon(R.drawable.ic_camera);
        setTitleLeftIcon(R.drawable.back_left);
        presenter = new MomentPresenter(this);

        hostViewHolder = new HostViewHolder(this);
        circleRecyclerView = (CircleRecyclerView) findViewById(R.id.recycler);
        circleRecyclerView.setOnRefreshListener(this);
        circleRecyclerView.setOnPreDispatchTouchListener(this);
        circleRecyclerView.addHeaderView(hostViewHolder.getView());

        mTipsLayout = findViewById(R.id.tips_layout);
        mServiceTipsView = (TextView) findViewById(R.id.service_tips);
        mCloseImageView = (ImageView) findViewById(R.id.iv_close);

        mUpdateTipsView = (TextView) findViewById(R.id.update_tips);
        mUpdateLayout = findViewById(R.id.update_layout);

        commentBox = (CommentBox) findViewById(R.id.widget_comment);
        commentBox.setOnCommentSendClickListener(onCommentSendClickListener);

        adapter = new CircleMomentsAdapter(this, momentsInfoList, presenter);
        adapter.addViewHolder(EmptyMomentsVH.class, MomentsType.EMPTY_CONTENT)
                .addViewHolder(MultiImageMomentsVH.class, MomentsType.MULTI_IMAGES)
                .addViewHolder(TextOnlyMomentsVH.class, MomentsType.TEXT_ONLY)
                .addViewHolder(WebMomentsVH.class, MomentsType.WEB);
        circleRecyclerView.setAdapter(adapter);
        circleRecyclerView.autoRefresh();

        TitleBarAlphaChangeHelper.handle(getTitleBar(),
                circleRecyclerView.getRecyclerView(),
                hostViewHolder.friend_avatar,
                new TitleBarAlphaChangeHelper.OnTitleBarAlphaColorChangeListener() {
                    @Override
                    public void onChange(float alpha, int color) {
                        setStatusBarDark(alpha > 1);
                        setStatusBarHolderBackgroundColor(color);
                    }
                });

    }

    private void initKeyboardHeightObserver() {
        //观察键盘弹出与消退
        KeyboardControlMnanager.observerKeyboardVisibleChange(this, new KeyboardControlMnanager.OnKeyboardStateChangeListener() {
            View anchorView;

            @Override
            public void onKeyboardChange(int keyboardHeight, boolean isVisible) {
                int commentType = commentBox.getCommentType();
                if (isVisible) {
                    //定位评论框到view
                    commentBox.setTranslationY(-keyboardHeight + commentBox.getHeight() + UIHelper.getStatusBarHeight(FriendCircleDemoActivity.this));
                    anchorView = mViewHelper.alignCommentBoxToView(circleRecyclerView, commentBox, commentType);
                } else {
                    //定位到底部
                    commentBox.setTranslationY(0);
                    commentBox.dismissCommentBox(false);
                    mViewHelper.alignCommentBoxToViewWhenDismiss(circleRecyclerView, commentBox, commentType, anchorView);
                }
            }
        });
    }

    @Override
    protected boolean isTranslucentStatus() {
        return true;
    }

    @Override
    protected boolean isFitsSystemWindows() {
        return false;
    }

    @Override
    public void onRefresh() {
        momentsRequest.setOnResponseListener(momentsRequestCallBack);
        momentsRequest.setRequestType(REQUEST_REFRESH);
        momentsRequest.setCurPage(0);
        momentsRequest.execute();
    }

    @Override
    public void onLoadMore() {
        momentsRequest.setOnResponseListener(momentsRequestCallBack);
        momentsRequest.setRequestType(REQUEST_LOADMORE);
        momentsRequest.execute();
    }


    //titlebar click


    @Override
    public boolean onTitleLongClick(View v) {
        new PopupTextAction(this)
                .setTitle("开发工具")
                .setTitleColor(UIHelper.getColor(R.color.text_gray))
                .addData("跳转到Fragment朋友圈", 1)
                .setOnSelectedListener(new PopupTextAction.OnActionClickedListener() {
                    @Override
                    public void onClicked(CharSequence action, int actionCode) {
                        ActivityLauncher.startToFriendCircleFragmentDemoActivity(FriendCircleDemoActivity.this);
                    }
                })
                .showPopupWindow();
        return super.onTitleLongClick(v);
    }

    @Override
    public void onTitleDoubleClick() {
        super.onTitleDoubleClick();
        if (circleRecyclerView != null) {
            int firstVisibleItemPos = circleRecyclerView.findFirstVisibleItemPosition();
            circleRecyclerView.getRecyclerView().smoothScrollToPosition(0);
            if (firstVisibleItemPos > 1) {
                circleRecyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        circleRecyclerView.autoRefresh();
                    }
                }, 200);
            }
        }

    }

    @Override
    public void onTitleLeftClick() {
        if (System.currentTimeMillis() - lastClickBackTime > 2000) { // 后退阻断
            UIHelper.ToastMessage("这是朋友圈工程哦，不是整个微信哦~再点一次退出");
            lastClickBackTime = System.currentTimeMillis();
        } else { // 关掉app
            super.onBackPressed();
        }
    }

    @Override
    public void onTitleRightClick() {
        new SelectPhotoMenuPopup(this).setOnSelectPhotoMenuClickListener(new SelectPhotoMenuPopup.OnSelectPhotoMenuClickListener() {
            @Override
            public void onShootClick() {
                PhotoHelper.fromCamera(FriendCircleDemoActivity.this, false);
            }

            @Override
            public void onAlbumClick() {
                ActivityLauncher.startToPhotoSelectActivity(getActivity(), RouterList.PhotoSelectActivity.requestCode);
            }
        }).showPopupWindow();
    }

    @Override
    public boolean onTitleRightLongClick() {
        ActivityLauncher.startToPublishActivityWithResult(this, RouterList.PublishActivity.MODE_TEXT, null, RouterList.PublishActivity.requestCode);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        PhotoHelper.handleActivityResult(this, requestCode, resultCode, data, new PhotoHelper.PhotoCallback() {
            @Override
            public void onFinish(String filePath) {
                List<ImageInfo> selectedPhotos = new ArrayList<ImageInfo>();
                selectedPhotos.add(new ImageInfo(filePath, null, null, 0, 0));
                ActivityLauncher.startToPublishActivityWithResult(FriendCircleDemoActivity.this,
                        RouterList.PublishActivity.MODE_MULTI,
                        selectedPhotos,
                        RouterList.PublishActivity.requestCode);
            }

            @Override
            public void onError(String msg) {
                UIHelper.ToastMessage(msg);
            }
        });
        if (requestCode == RouterList.PhotoSelectActivity.requestCode && resultCode == RESULT_OK) {
            List<ImageInfo> selectedPhotos = data.getParcelableArrayListExtra(RouterList.PhotoSelectActivity.key_result);
            if (selectedPhotos != null) {
                ActivityLauncher.startToPublishActivityWithResult(this, RouterList.PublishActivity.MODE_MULTI, selectedPhotos, RouterList.PublishActivity.requestCode);
            }
        }

        if (requestCode == RouterList.PublishActivity.requestCode && resultCode == RESULT_OK) {
            circleRecyclerView.autoRefresh();
        }
    }

    //request
    //==============================================
    private OnResponseListener.SimpleResponseListener<List<MomentsInfo>> momentsRequestCallBack = new OnResponseListener.SimpleResponseListener<List<MomentsInfo>>() {
        @Override
        public void onSuccess(List<MomentsInfo> response, int requestType) {
            circleRecyclerView.compelete();
            switch (requestType) {
                case REQUEST_REFRESH:
                    if (!ToolUtil.isListEmpty(response)) {
                        KLog.i("firstMomentid", "第一条动态ID   >>>   " + response.get(0).getMomentid());
                        hostViewHolder.loadHostData(LocalHostManager.INSTANCE.getLocalHostUser());
                        adapter.updateData(response);
                    }
                    checkRegister();
                    break;
                case REQUEST_LOADMORE:
                    adapter.addMore(response);
                    break;
            }
        }

        @Override
        public void onError(BmobException e, int requestType) {
            super.onError(e, requestType);
            circleRecyclerView.compelete();
        }
    };


    //=============================================================View's method
    @Override
    public void onLikeChange(int itemPos, List<LikesInfo> likeUserList) {
        MomentsInfo momentsInfo = adapter.findData(itemPos);
        if (momentsInfo != null) {
            momentsInfo.setLikesList(likeUserList);
            adapter.notifyItemChanged(itemPos);
        }
    }

    @Override
    public void onCommentChange(int itemPos, List<CommentInfo> commentInfoList) {
        MomentsInfo momentsInfo = adapter.findData(itemPos);
        if (momentsInfo != null) {
            momentsInfo.setCommentList(commentInfoList);
            adapter.notifyItemChanged(itemPos);
        }
    }

    @Override
    public void showCommentBox(@Nullable View viewHolderRootView, int itemPos, String momentid, CommentWidget commentWidget) {
        if (viewHolderRootView != null) {
            mViewHelper.setCommentAnchorView(viewHolderRootView);
        } else if (commentWidget != null) {
            mViewHelper.setCommentAnchorView(commentWidget);
        }
        mViewHelper.setCommentItemDataPosition(itemPos);
        commentBox.toggleCommentBox(momentid, commentWidget == null ? null : commentWidget.getData(), false);
    }

    @Override
    public void onDeleteMomentsInfo(@NonNull MomentsInfo momentsInfo) {
        int pos = adapter.getDatas().indexOf(momentsInfo);
        if (pos < 0) return;
        adapter.deleteData(pos);
    }

    @Override
    public boolean onPreTouch(MotionEvent ev) {
        if (commentBox != null && commentBox.isShowing()) {
            commentBox.dismissCommentBox(false);
            return true;
        }
        return false;
    }

    //=============================================================tool method
    private void checkRegister() {
        boolean hasCheckRegister = (boolean) AppSetting.loadBooleanPreferenceByKey(AppSetting.CHECK_REGISTER, false);
        if (!hasCheckRegister) {
            RegisterPopup registerPopup = new RegisterPopup(FriendCircleDemoActivity.this);
            registerPopup.setOnRegisterSuccess(new RegisterPopup.onRegisterSuccess() {
                @Override
                public void onSuccess(UserInfo userInfo) {
                    hostViewHolder.loadHostData(userInfo);
                    UpdateInfoManager.INSTANCE.showUpdateInfo();
                }
            });
            registerPopup.showPopupWindow();
        } else {
            UpdateInfoManager.INSTANCE.showUpdateInfo();
        }
    }

    private long lastClickBackTime;

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - lastClickBackTime > 2000) { // 后退阻断
            UIHelper.ToastMessage("再点一次退出");
            lastClickBackTime = System.currentTimeMillis();
        } else { // 关掉app
            super.onBackPressed();
        }
    }


    //服务器消息检查，非项目所需↓
    @SuppressLint("CheckResult")
    private void delayCheckServiceInfo() {
        Observable.timer(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        checkServiceInfo();
                        checkUpdateInfo();
                    }
                });
    }

    private void checkUpdateInfo() {
        UpdateInfoManager.INSTANCE.checkUpdate(new SimpleCallback<UpdateInfo>() {
            @Override
            public void onCall(final UpdateInfo data) {
                mUpdateTipsView.setText(String.format("新版本(%1$s)已经发布，点击查看更新日志并更新。", data.getVersion()));
                mUpdateTipsView.setOnClickListener(new SingleClickListener() {
                    @Override
                    public void onSingleClick(View v) {
                        toUpdate(data);
                    }
                });
                mUpdateLayout.setTranslationY(UIHelper.dipToPx(50));
                mUpdateLayout.animate()
                        .setStartDelay(300)
                        .alpha(1f)
                        .translationY(0)
                        .setDuration(800)
                        .setInterpolator(new DecelerateInterpolator())
                        .setListener(new AnimUtils.SimpleAnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                mUpdateLayout.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mUpdateTipsView.requestFocus();
                            }
                        }).start();
            }
        });
    }

    private void toUpdate(UpdateInfo data) {
        PopupUpdate popupUpdate = new PopupUpdate(this);
        popupUpdate.showPopupWindow(data);
    }

    private void checkServiceInfo() {
        ServiceInfoManager.INSTANCE.check(new ServiceInfoManager.OnCheckServiceInfoListener() {
            @Override
            public void onCheckFinish(@Nullable final ServiceInfo serviceInfo) {
                if (serviceInfo != null) {
                    mServiceTipsView.setText(serviceInfo.getTips());
                    mServiceTipsView.setOnClickListener(new SingleClickListener() {
                        @Override
                        public void onSingleClick(View v) {
                            ActivityLauncher.startToServiceInfoActivity(FriendCircleDemoActivity.this, serviceInfo);
                            clickServiceCount++;
                            applyClose();
                        }
                    });
                    mTipsLayout.setTranslationY(UIHelper.dipToPx(50));
                    mTipsLayout.animate()
                            .alpha(1f)
                            .translationY(0)
                            .setDuration(800)
                            .setInterpolator(new DecelerateInterpolator())
                            .setListener(new AnimUtils.SimpleAnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {
                                    mTipsLayout.setVisibility(View.VISIBLE);
                                }
                            }).start();
                }
            }
        });

    }

    private void applyClose() {
        if (clickServiceCount < 1) return;
        mCloseImageView.setImageResource(R.drawable.ic_close_white);
        mCloseImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTipsLayout.animate()
                        .alpha(0)
                        .translationY(0)
                        .setDuration(800)
                        .setInterpolator(new DecelerateInterpolator())
                        .setListener(new AnimUtils.SimpleAnimatorListener() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mTipsLayout.setVisibility(View.GONE);
                            }
                        }).start();
            }
        });
    }
    //服务器消息检查，非项目所需↑

    //=============================================================call back
    private CommentBox.OnCommentSendClickListener onCommentSendClickListener = new CommentBox.OnCommentSendClickListener() {
        @Override
        public void onCommentSendClick(View v, IComment comment, String commentContent) {
            if (TextUtils.isEmpty(commentContent)) {
                commentBox.dismissCommentBox(true);
                return;
            }
            int itemPos = mViewHelper.getCommentItemDataPosition();
            if (itemPos < 0 || itemPos > adapter.getItemCount()) return;
            List<CommentInfo> commentInfos = adapter.findData(itemPos).getCommentList();
            String userid = (comment instanceof CommentInfo) ? ((CommentInfo) comment).getAuthor().getUserid() : null;
            presenter.addComment(itemPos, commentBox.getMomentid(), userid, commentContent, commentInfos);
            commentBox.clearDraft();
            commentBox.dismissCommentBox(true);
        }
    };


    private static class HostViewHolder {
        private View rootView;
        private ImageView friend_wall_pic;
        private ImageView friend_avatar;
        private ImageView message_avatar;
        private TextView message_detail;
        private TextView hostid;

        public HostViewHolder(Context context) {
            this.rootView = LayoutInflater.from(context).inflate(R.layout.circle_host_header, null);
            this.hostid = (TextView) rootView.findViewById(R.id.host_id);
            this.friend_wall_pic = (ImageView) rootView.findViewById(R.id.friend_wall_pic);
            this.friend_avatar = (ImageView) rootView.findViewById(R.id.friend_avatar);
            this.message_avatar = (ImageView) rootView.findViewById(R.id.message_avatar);
            this.message_detail = (TextView) rootView.findViewById(R.id.message_detail);
        }

        public void loadHostData(UserInfo hostInfo) {
            if (hostInfo == null) return;
            ImageLoadMnanger.INSTANCE.loadImage(friend_wall_pic, hostInfo.getCover());
            ImageLoadMnanger.INSTANCE.loadImage(friend_avatar, hostInfo.getAvatar());
            hostid.setText(hostInfo.getNick());
        }

        public View getView() {
            return rootView;
        }

    }
}
