package us.wearecurio.model

import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

import java.math.MathContext
import java.sql.ResultSet
import java.util.TreeSet
import us.wearecurio.utility.Utils
import us.wearecurio.services.DatabaseService
import us.wearecurio.services.AlertGenerationService
import us.wearecurio.model.Entry
import us.wearecurio.data.RepeatType
import us.wearecurio.data.DataRetriever
import java.util.Date

import grails.compiler.GrailsCompileStatic

public class AlertNotification {

	private static Log log = LogFactory.getLog(this)

	Long userId
	Date date
	Long objectId
	String text
	
	static constraints = {
		objectId(nullable:true)
	}
	static mapping = {
		version false
		table 'alert_notification'
		userId column:'user_id', index:'user_id_index'
		objectId column:'object_id', index:'object_id_index'
		date column:'date', index:'date_index'
	}

	AlertNotification() {
	}
	
	static final MathContext mc = new MathContext(9)
	
	static def createOrUpdate(Long userId, Date date, Long objectId, String text) {
		log.debug "createOrUpdate() userId:" + userId + ", date:" + date + ", objectId:" + objectId + ", text:" + text
		
		AlertNotification alert = AlertNotification.findByUserIdAndObjectIdAndDate(userId, objectId, date)
		
		if (!alert) {
			alert = new AlertNotification(userId:userId, date:date, objectId:objectId, text:text)
		}
		
		Utils.save(alert, true)
		
		return alert
	}
	
	static def generate(Long userId, Date startDate, Date endDate) {
		AlertNotification.withTransaction {
			deleteforUserBetween(userId, startDate, endDate)
			def remindData = DataRetriever.get().fetchRemindData(userId, startDate, endDate, new Date())
			for (result in remindData) {
				createOrUpdate(userId, result[0], result[1],  result[2])
			}
		}
	}
	
	static def deleteforUser(Long userId) {
		AlertNotification.executeUpdate("delete AlertNotification a where a.userId = :userId", [userId:userId])
	}
	
	static def deleteforUserBetween(Long userId, Date startDate, Date endDate) {
		log.debug "deleteforUser() userId:" + userId + ", startDate:" + startDate + ", endDate:" + endDate
		
		AlertNotification.executeUpdate("delete AlertNotification a where a.userId = :userId and a.date >= :startDate and a.date < :endDate",
				[userId:userId, startDate:startDate, endDate:endDate])
	}
	
	static def pendingAlerts(Long userId, Date startDate, Date endDate) {
		def alerts = AlertNotification.executeQuery("from AlertNotification a where userId = :userId and a.date >= :startDate and a.date < :endDate",
				[userId:userId, startDate:startDate, endDate:endDate])
		
		if (alerts?.size()) {
			log.debug "pendingAlerts() userId:" + userId + ", startDate:" + startDate + ", endDate:" + endDate + " alerts:" + alerts
		}
		
		return alerts
	}

	String toString() {
		return "AlertNotification(userId:" + userId + ", objectId:" + objectId + ", date:" + date + ", text:" \
				+ text + ")"
	}
}
