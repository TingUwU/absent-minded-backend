package absent_minded.absent_minded.services;

import absent_minded.absent_minded.models.Project;
import absent_minded.absent_minded.models.Task;
import absent_minded.absent_minded.repositories.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class TaskService {

    private final TaskRepository repo;
    private final AuthService auth;
    private final ProjectService projectService;

    public TaskService(
            TaskRepository repo,
            AuthService auth,
            ProjectService projectService) {
        this.repo = repo;
        this.auth = auth;
        this.projectService = projectService;
    }

    public List<Task> getTasksByProject(String header, String projectId) {
        Project project = projectService.getProjectById(header, projectId);
        if (project == null) {
            return Collections.emptyList();
        }
        return repo.findAllByProject(projectId);
    }

    public List<Task> getAllTasks(String header) {
        List<Project> projects = projectService.getAllProjects(header);
        List<String> projectIds = projects.stream()
                .map(Project::getId)
                .toList();
        return repo.findAllByProjectIn(projectIds);
    }

    public List<Task> createTasks(String header, List<Task> tasks) {
        verifyVisitorByTasks(header, tasks);
        return repo.saveAll(tasks);
    }

    public List<Task> updateTasks(String header, List<Task> tasks) {
        verifyVisitorByTasks(header, tasks);
        return repo.saveAll(tasks);
    }

    public void deleteTasks(String header, List<String> ids) {
        verifyVisitorByIds(header, ids);
        repo.deleteAllById(ids);
    }

    private void verifyVisitorByTasks(String header, List<Task> tasks) {
        String email = auth.emailFromAuthHeader(header);
        tasks.forEach(task -> verifyProjectAccess(header, task.getProject(), email));
    }

    private void verifyVisitorByIds(String header, List<String> ids) {
        String email = auth.emailFromAuthHeader(header);
        List<Task> tasks = repo.findAllById(ids);
        if (tasks.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
        tasks.forEach(task -> verifyProjectAccess(header, task.getProject(), email));
    }

    private void verifyProjectAccess(String header, String projectId, String email) {
        Project project = projectService.getProjectById(header, projectId);
        if (project == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "NO access");
        }
        List<String> participants = project.getParticipants();
        boolean isParticipant = participants != null && participants.contains(email);
        if (!project.getOwnerId().equals(email) && !isParticipant) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "NO access");
        }
    }
}
