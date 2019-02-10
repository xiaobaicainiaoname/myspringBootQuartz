package com.example.demo.service.impl;

import com.example.demo.job.AsyncJob;
import com.example.demo.job.CronJob;
import com.example.demo.service.JobService;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * @author hzb
 * @date 2018/08/28
 */
@Service
public class JobServiceImpl implements JobService {

    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;

    /**
     * 创建一个定时任务
     *
     * @param jobName
     * @param jobGroup
     */
    @Override
    public void addCronJob(String jobName, String jobGroup) {
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            if (jobDetail != null) {
                System.out.println("job:" + jobName + " 已存在  用来修改");
                JobDetail jobDetail2 = jobDetail.getJobBuilder().withDescription("修改").build();
                
                JobDataMap jobDataMap = jobDetail.getJobDataMap();
                jobDataMap.put("taskData", "啊啊");
                
               
//                TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);
                TriggerKey triggerKey = TriggerKey.triggerKey(jobName + "_trigger", jobGroup + "_trigger");
    			// 表达式调度构建器
    			CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("*/2 * * * * ?");

    			CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);

    			// 按新的cronExpression表达式重新构建trigger
    			trigger = trigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(scheduleBuilder).build();

    			// 按新的trigger重新设置job执行
//    			scheduler.rescheduleJob(triggerKey, trigger);
    			
    			 scheduler.deleteJob(jobKey);
    			scheduler.scheduleJob(jobDetail2, trigger);
    			
            } else {
                //构建job信息
                jobDetail = JobBuilder.newJob(CronJob.class).withIdentity(jobName, jobGroup)
                		.withDescription("jobDetail+job功能描述").build();
                //用JopDataMap来传递数据
                JobDataMap jobDataMap = jobDetail.getJobDataMap();
                jobDataMap.put("taskData", "hzb-cron-001");
                jobDataMap.put("url", "https://blog.csdn.net/xiaohuaidan007/article/details/77485673?utm_source=blogxgwz0");

                //表达式调度构建器(即任务执行的时间,每5秒执行一次)
                CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("*/5 * * * * ?");

                //按新的cronExpression表达式构建一个新的trigger
                CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(jobName + "_trigger", jobGroup + "_trigger")
                        .withSchedule(scheduleBuilder).withDescription("trigger+描述").build();
                scheduler.scheduleJob(jobDetail, trigger);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 直接查表写sql
     */
    @Override
	public void queryJob(String jobName1, String jobGroup1) {
    	HashMap<Object, Object> hashMap = new HashMap<>();
    	try {
    		Scheduler scheduler = schedulerFactoryBean.getScheduler();
//        	schedulerFactoryBean.
             for (String groupName : scheduler.getJobGroupNames()) {
                 for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                     String jobName = jobKey.getName();
                     String jobGroup = jobKey.getGroup();
                     //get job's trigger
//                     List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
//                     Date nextFireTime = triggers.get(0).getNextFireTime();
//                     System.out.println("[jobName] : " + jobName + " [groupName] : "
//                         + jobGroup + " - " + nextFireTime);
                     JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                     String description = jobDetail.getDescription();
                     JobDataMap jobDataMap = jobDetail.getJobDataMap();
//                     jobDetail.
                     List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
//                     triggers.get(0).getStartTime()
                 }
             }
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
	}
    @Override
    public void addAsyncJob(String jobName, String jobGroup) {
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();

            JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            if (jobDetail != null) {
                System.out.println("job:" + jobName + " 已存在");
            }
            else {
                //构建job信息,在用JobBuilder创建JobDetail的时候，有一个storeDurably()方法，可以在没有触发器指向任务的时候，将任务保存在队列中了。然后就能手动触发了
                jobDetail = JobBuilder.newJob(AsyncJob.class).withIdentity(jobName, jobGroup).storeDurably().build();
                jobDetail.getJobDataMap().put("asyncData","this is a async task");
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity(jobName + "_trigger", jobGroup + "_trigger") //定义name/group
                        .startNow()//一旦加入scheduler，立即生效
                        .withSchedule(simpleSchedule())//使用SimpleTrigger
                        .build();
                scheduler.scheduleJob(jobDetail, trigger);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void pauseJob(String jobName, String jobGroup) {
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            TriggerKey triggerKey = TriggerKey.triggerKey(jobName + "_trigger", jobGroup + "_trigger");

            scheduler.pauseTrigger(triggerKey);
            System.out.println("=========================pause job:" + jobName + " success========================");
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    /**
     * 恢复任务
     *
     * @param jobName
     * @param jobGroup
     */
    @Override
    public void resumeJob(String jobName, String jobGroup) {
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            TriggerKey triggerKey = TriggerKey.triggerKey(jobName + "_trigger", jobGroup + "_trigger");
            scheduler.resumeTrigger(triggerKey);
            System.out.println("=========================resume job:" + jobName + " success========================");
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteJob(String jobName, String jobGroup) {
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            JobKey jobKey = JobKey.jobKey(jobName,jobGroup);
            scheduler.deleteJob(jobKey);
            System.out.println("=========================delete job:" + jobName + " success========================");
        } catch (SchedulerException e) {
            e.printStackTrace();
        }

    }

	
}