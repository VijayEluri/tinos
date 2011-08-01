package rina.enrollment.impl;

import java.util.Hashtable;
import java.util.Map;

import rina.enrollment.api.EnrollmentTask;
import rina.enrollment.api.EnrollmentTaskFactory;
import rina.ipcservice.api.ApplicationProcessNamingInfo;

public class EnrollmentTaskFactoryImpl implements EnrollmentTaskFactory{
	
	private Map<ApplicationProcessNamingInfo, EnrollmentTask> enrollmentTaskRespository = null;
	
	public EnrollmentTaskFactoryImpl(){
		enrollmentTaskRespository = new Hashtable<ApplicationProcessNamingInfo, EnrollmentTask>();
	}

	public EnrollmentTask createEnrollmentTask(ApplicationProcessNamingInfo apNamingInfo) {
		EnrollmentTask enrollmentTask = new EnrollmentTaskImpl();
		enrollmentTaskRespository.put(apNamingInfo, enrollmentTask);
		return enrollmentTask;
	}

	public void destroyEnrollmentTask(ApplicationProcessNamingInfo apNamingInfo) {
		enrollmentTaskRespository.remove(apNamingInfo);
	}

	public EnrollmentTask getEnrollmentTask(ApplicationProcessNamingInfo apNamingInfo) {
		return enrollmentTaskRespository.get(apNamingInfo);
	}

}