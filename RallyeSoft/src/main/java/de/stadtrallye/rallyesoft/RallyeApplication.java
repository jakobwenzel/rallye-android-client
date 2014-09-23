/*
 * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
 *
 * This file is part of RallyeSoft.
 *
 * RallyeSoft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
 */

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
				.showImageOnLoading(R.drawable.ic_stub)
				.cacheInMemory(true)
				.cacheOnDisk(true)
				.build();

		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
//				.discCache(new TotalSizeLimitedDiscCache(cacheDir, 100 * 1024 * 1024))
				.diskCacheSize(100 * 1024 * 1024)
				.defaultDisplayImageOptions(disp)
				.build();

		ImageLoader.getInstance().init(config);
	}
}
