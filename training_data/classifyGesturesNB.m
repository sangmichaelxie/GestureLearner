
function classifyGesturesNB
    clear all; clear;
    
    %Using the larger test data for training increases performance
    O = load('O_test.txt');
    X = load('X_test.txt');
    Z = load('Z_test.txt');
    num_features = size(O ,2);
    %plotGestureData(O, 1);
    %plotGestureData(X, 2);
    %plotGestureData(Z, 3);
    
    training_instance_matrix = [O; X; Z;];
    
    size(training_instance_matrix)
    
    training_label_vector = [zeros(size(O, 1), 1); ones(size(X, 1), 1); 2 * ones(size(Z, 1), 1);];
    
    NBModel = fitNaiveBayes(training_instance_matrix, training_label_vector);
    
    
    
    %radial basis (gaussian) SVM
    %model = svmtrain(training_label_vector, training_instance_matrix, '-s 0 -t 2')
    %train_predictions = svmpredict(training_label_vector, training_instance_matrix, model)
    %test(model);
    
    test(NBModel)
    
end


function test(model)
    O_test = load('O.txt');
    X_test = load('X.txt');
    Z_test = load('Z.txt');
    
    testing_instance_matrix = [O_test; X_test; Z_test;];
    testing_label_vector = [zeros(size(O_test, 1), 1); ones(size(X_test, 1), 1); 2 * ones(size(Z_test, 1), 1);];
    
    model.predict(testing_instance_matrix) - testing_label_vector
    
    size(testing_label_vector)
    
    %[test_predictions, accuracy, decision_values] = svmpredict(testing_label_vector, testing_instance_matrix, model)

end

function [X,Y,Z] = splitData(G)
    X = G(:, 1:100);
    Y = G(:, 101:200);
    Z = G(:, 201:300); 
end

function plotGestureData(G, figure_count)
    figure_num = (figure_count - 1) * 2 + 1;
    figure(figure_num);
    [X,Y,Z] = splitData(G);
    for i = 1:size(X,1)
        plot3(X(i,:),Y(i,:),Z(i,:));
        hold on;
    end
    title('All training examples');
    hold off;
    
    figure(figure_num + 1);
    plot3(X(1,:),Y(1,:),Z(1,:));
    title('One(first) training example');
    
end