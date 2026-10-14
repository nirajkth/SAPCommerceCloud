In your log file (

console-20261014.log
), the following 3 Solr indexing cronjobs are executing every 1 minute on a recurring schedule:

update-electronicsIndex-cronJob
update-powertoolsIndex-cronJob
update-backofficeIndex-CronJob
Each execution triggers index updates, user session switches, and frequent [Slf4jAuditableActionHandler] DB_AUDIT logs. Furthermore, your 

local.properties
 has task.polling.interval.min=0, causing the Task Engine to poll continuously with zero delay.

Here is how you can stop them immediately and permanently:

Option 1: Stop Them Immediately via HAC (No Server Restart Required)
If your server is already running and you want to stop the log spam right away:

A. Via HAC ImpEx Import
Go to HAC (https://localhost:9002) $\rightarrow$ Console $\rightarrow$ ImpEx Import.
Run the following ImpEx to deactivate the triggers for those indexing jobs:
impex
UPDATE Trigger; cronjob(code)[unique=true]; active
; update-electronicsIndex-cronJob ; false
; update-powertoolsIndex-cronJob ; false
; update-backofficeIndex-CronJob ; false
(Tip: If you want to disable all cronjob triggers across your local environment at once, run:)

impex
UPDATE Trigger; active[default=false]; active
; true ; false
B. Via HAC Groovy Script
Go to HAC $\rightarrow$ Console $\rightarrow$ Scripting Languages $\rightarrow$ select Groovy, and run:

groovy
import de.hybris.platform.servicelayer.cronjob.CronJobService
import de.hybris.platform.servicelayer.model.ModelService
def cronJobService = spring.getBean("cronJobService")
def modelService = spring.getBean("modelService")
def jobs = ["update-electronicsIndex-cronJob", "update-powertoolsIndex-cronJob", "update-backofficeIndex-CronJob"]
jobs.each { jobCode ->
    try {
        def cronJob = cronJobService.getCronJob(jobCode)
        cronJob.getTriggers().each { trigger ->
            trigger.setActive(false)
            modelService.save(trigger)
            println "Deactivated trigger for: ${jobCode}"
        }
    } catch (Exception e) {
        println "Error processing ${jobCode}: ${e.message}"
    }
}
Option 2: Stop All CronJobs Automatically on Startup (Recommended for Local Dev)
In a local development environment, running background cronjobs automatically on startup is rarely needed and wastes CPU and RAM.

Open 

i:/COMCL2211/hybris/config/local.properties
.
Add the following property:
properties
# Disable task engine & automatic cronjob trigger executions on startup
task.engine.loadonstartup=false
(Optional but recommended): Check line 51 in 

local.properties
:
properties
task.polling.interval.min=0
Either comment this out or set it back to the default task.polling.interval.min=10. Setting it to 0 causes the task engine to hammer the DB constantly.
NOTE

Setting task.engine.loadonstartup=false will prevent automated triggers from running on startup. You can still trigger any CronJob manually whenever needed via HAC or Backoffice.

Option 3: Via Backoffice (GUI)
Log into Backoffice (https://localhost:9002/backoffice) as an admin.
Navigate to System $\rightarrow$ CronJobs.
Search for:
update-electronicsIndex-cronJob
update-powertoolsIndex-cronJob
update-backofficeIndex-CronJob
For each CronJob, go to the Triggers tab and set Active to False (or delete the trigger).
Option 4: Silence the Audit & Indexer Loggers (If you want to keep them running silently)
If you need the jobs running but want to mute the logs:

In HAC $\rightarrow$ Monitoring $\rightarrow$ Log4J / Loggers:
de.hybris.platform.audit.internal.action.Slf4jAuditableActionHandler $\rightarrow$ set to WARN (silences DB_AUDIT spam)
de.hybris.platform.solrfacetsearch $\rightarrow$ set to WARN
To make it persistent across restarts, add to 

local.properties
:
properties
log4j2.logger.auditaction.name=de.hybris.platform.audit.internal.action.Slf4jAuditableActionHandler
log4j2.logger.auditaction.level=WARN
log4j2.logger.solrindexer.name=de.hybris.platform.solrfacetsearch
log4j2.logger.solrindexer.level=WARN
5:21 PM
Slf4jAuditableActionHandler still info is coming after setting it to warning
8:36 PM
The reason INFO logs are still appearing comes down to two main causes:

Exact Package Name: The full class package is: 👉 de.hybris.platform.audit.actions.impl.Slf4jAuditableActionHandler (Note the actions.impl path. If you configured it without this exact full package, Log4j2 won't match the logger.)

local.properties requires a restart: If you added it to local.properties, Log4j2 does not reload properties on a running server dynamically. It only takes effect after a restart.

Solution 1: Change it Instantly via HAC Groovy (No Restart Needed)
You can force Log4j2 to change the level in memory right now:

Open HAC (https://localhost:9002) $\rightarrow$ Console $\rightarrow$ Scripting Languages.
Select Groovy and run:
groovy
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
def loggerName = "de.hybris.platform.audit.actions.impl.Slf4jAuditableActionHandler"
def pkgName = "de.hybris.platform.audit"
println "Previous Level: " + LogManager.getLogger(loggerName).getLevel()
// Set the specific class and the entire audit package to WARN
Configurator.setLevel(loggerName, Level.WARN)
Configurator.setLevel(pkgName, Level.WARN)
println "New Level for " + loggerName + ": " + LogManager.getLogger(loggerName).getLevel()
println "New Level for " + pkgName + ": " + LogManager.getLogger(pkgName).getLevel()
The output will confirm New Level: WARN, and the DB_AUDIT logs will stop immediately.

Solution 2: Change it via HAC UI
Go to HAC $\rightarrow$ Monitoring $\rightarrow$ Log4J.
Scroll to the Configure log levels section at the bottom.
In the Logger Name field, paste:
text
de.hybris.platform.audit.actions.impl.Slf4jAuditableActionHandler
Set the dropdown to WARN.
Click Change level.
Repeat for the parent package de.hybris.platform.audit to catch any related audit loggers.
Solution 3: Make it Permanent in local.properties
Add the following to 

i:/COMCL2211/hybris/config/local.properties
:

properties
# Mute DB_AUDIT logs from Slf4jAuditableActionHandler
log4j2.logger.auditaction.name=de.hybris.platform.audit.actions.impl.Slf4jAuditableActionHandler
log4j2.logger.auditaction.level=WARN
# Mute all platform audit logs
log4j2.logger.auditall.name=de.hybris.platform.audit
log4j2.logger.auditall.level=WARN
Recommended: Deactivate the Recurring Cronjob Triggers
Even with the audit logs silenced, the 3 Solr indexing cronjobs (update-electronicsIndex-cronJob, update-powertoolsIndex-cronJob, update-backofficeIndex-CronJob) are still actively running in the background every 60 seconds, which will continue to generate other logs and consume CPU.

To stop them from firing in the background entirely, run this in HAC $\rightarrow$ Console $\rightarrow$ ImpEx Import:

impex
UPDATE Trigger; cronjob(code)[unique=true]; active
; update-electronicsIndex-cronJob ; false
; update-powertoolsIndex-cronJob ; false
; update-backofficeIndex-CronJob ; false
8:36 PM
