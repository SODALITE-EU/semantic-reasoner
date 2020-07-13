package kb.optimization.exceptions;

import java.util.List;

import kb.optimization.exceptions.models.OptimizationModel;

public class OptimizationException extends Exception {
	private static final long serialVersionUID = 1L;

	public OptimizationException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}
	
	public List<OptimizationModel> optimizationModels;

	public OptimizationException(List<OptimizationModel> optimizationModels) {
		this.optimizationModels = optimizationModels;
	}
}
