package de.stadtrallye.rallyesoft;

import android.app.Application;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

/**
 * Configures the UniversalImageLoader for the entire Application (particularly the caching configuration)
 */
public class RallyeApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

//		File cacheDir = StorageUtils.getCacheDirectory(getApplicationContext());

		DisplayImageOptions disp = new DisplayImageOptions.Builder()
				.showImageOnFail(R.drawable.ic_empty)
				.showStubImage(R.drawable.ic_stub)
				.cacheInMemory(true)
				.cacheOnDisc(true)
				.build();

		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
//				.discCache(new TotalSizeLimitedDiscCache(cacheDir, 100 * 1024 * 1024))
				.discCacheSize(100 * 1024 * 1024)
				.defaultDisplayImageOptions(disp)

				.build();

		ImageLoader.getInstance().init(config);
	}
}
