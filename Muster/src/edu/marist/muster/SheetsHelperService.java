package edu.marist.muster;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;

import edu.marist.muster.SheetsHelper;

public class SheetsHelperService extends Service<String>{

	private SheetsHelper helper;
	private String email;
	
	public void setSheetsHelper(SheetsHelper h) {
		helper = h;
	}
	
	public SheetsHelper getSheetsHelper(SheetsHelper h) {
		return helper;
	}
	
	public void setEmail(String e) {
		email = e;
	}
	
	public String getEmail() {
		return email;
	}
	
	@Override
	protected Task<String> createTask() {
		SheetsHelper helper = this.helper;
		String email = this.email;
		return new Task<String>() {

			@Override
			protected String call() throws Exception {
				helper.mark(email);
				return email;
			}
			
		};
	}

}
