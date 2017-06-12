package busu.test3;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.api.services.books.model.Volume;
import com.squareup.picasso.Picasso;

import busu.mvvm.activity.BaseMvvmActivity;
import busu.mvvm.activity.RequiresActivityViewModel;
import busu.test3.gbooks.BooksUtilities;

@RequiresActivityViewModel(DetailAVM.class)
public class DetailActivity extends BaseMvvmActivity<DetailAVM> {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail);
        doSomeWiring();
    }

    private void doSomeWiring() {
        viewModel().outVolumeLoaded()
                .compose(bindToLifecycle())
                .subscribe(volume -> volumeLoaded(volume));

        viewModel().outError()
                .compose(bindToLifecycle())
                .subscribe(throwable -> printError(throwable));
    }

    private void volumeLoaded(@NonNull Volume volume) {
        hideLoading();
        loadCover(volume.getVolumeInfo());
        loadBookDetails(volume.getVolumeInfo());
    }

    private void printError(Throwable throwable) {
        hideLoading();
        ((TextView) findViewById(R.id.detail_text)).setText("An error occured: \n " + throwable.toString());
    }

    private void hideLoading() {
        findViewById(R.id.detail_loading).setVisibility(View.GONE);
    }

    private void loadCover(Volume.VolumeInfo volumeInfo) {
        String bigCoverUrlPath = BooksUtilities.getSuitableImageLink(volumeInfo, false);
        Picasso.with(null).load(bigCoverUrlPath).into((ImageView) findViewById(R.id.detail_cover));
    }

    private void loadBookDetails(Volume.VolumeInfo volumeInfo) {
        String details = BooksUtilities.generateBookDetails(volumeInfo);
        ((TextView) findViewById(R.id.detail_text)).setText(details);
    }
}
