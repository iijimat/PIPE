package pipe.views;

import pipe.gui.ApplicationSettings;
import pipe.gui.Constants;
import pipe.gui.PetriNetTab;
import pipe.gui.ZoomController;
import pipe.gui.widgets.ArcWeightEditorPanel;
import pipe.gui.widgets.EscapableDialog;
import pipe.historyActions.AddArcPathPoint;
import pipe.historyActions.ArcWeight;
import pipe.historyActions.HistoryItem;
import pipe.models.Arc;
import pipe.models.Connectable;
import pipe.models.PipeObservable;
import pipe.models.Place;
import pipe.models.interfaces.IObserver;
import pipe.utilities.Copier;
import pipe.views.viewComponents.ArcPath;
import pipe.views.viewComponents.ArcPathPoint;
import pipe.views.viewComponents.NameLabel;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public abstract class ArcView extends PetriNetViewComponent implements Cloneable, IObserver,Serializable, Observer
{
    private static final long serialVersionUID = 1L;
    LinkedList<NameLabel> weightLabel = new LinkedList<NameLabel>();
    LinkedList<MarkingView> _weight = new LinkedList<MarkingView>();

    private ConnectableView _source = null;
    private ConnectableView _target = null;

    private Arc _model;

    final ArcPath myPath = new ArcPath(this);

    // true if arc is not hidden when a bidirectional arc is used
    boolean inView = true;

    // bounds of arc need to be grown in order to avoid clipping problems
    final int zoomGrow = 10;
	private boolean _noFunctionalWeights=true;

    ArcView(double startPositionXInput, double startPositionYInput,
            double endPositionXInput, double endPositionYInput,
            ConnectableView sourceInput,
            ConnectableView targetInput, LinkedList<MarkingView> weightInput,
            String idInput, Arc model)
    {
        _model = model;

        //TODO: THIS IS MESSY IMPLEMENT BETTER
        Pair<Point2D.Double, Point2D.Double> linePoints = getArcStartAndEnd();
        myPath.addPoint((float) linePoints.first.x, (float) linePoints.first.y, ArcPathPoint.STRAIGHT);
        myPath.addPoint((float) linePoints.second.x, (float) linePoints.second.y, ArcPathPoint.STRAIGHT);
        myPath.createPath();

        updateBounds();
        _id = idInput;
        setSource(sourceInput);
        setTarget(targetInput);
        setWeight(Copier.mediumCopy(weightInput));
    }

    private Pair<Point2D.Double, Point2D.Double> getArcStartAndEnd()
    {
        Connectable source = _model.getSource();
        Connectable target = _model.getTarget();
        double deltaX = source.getX() - target.getX();
        double deltaY = source.getY() -  target.getY();
        double angle = Math.atan(deltaX / deltaY);
        if (source.getY() <= target.getY())
        {
            angle += Math.PI;
        }

        Point2D.Double  startCoord;
        if (source.getClass().equals(Place.class))
        {
            startCoord = getPlaceCoordinates(source, angle);
        } else
        {
            startCoord = getTransitionCoordinatesSource(source, angle);
        }

        Point2D.Double  endCoord;
        if (target.getClass().equals(Place.class))
        {
            endCoord = getPlaceCoordinates(target, angle);
        } else
        {
            endCoord = getTransitionCoordinatesDest(target, angle);
        }
        return new Pair<Point2D.Double, Point2D.Double>(startCoord, endCoord);
    }

    private static class Pair<T, T2>
    {
        public final T first;
        public final T2 second;
        public Pair(T first, T2 second) {
            this.first = first;
            this.second = second;
        }
    }

    private Point2D.Double getPlaceCoordinates(Connectable connectable, double angle)
    {
        double radius = connectable.getWidth()/2;
        double zoomed_centre_x = ZoomController.getZoomedValue(connectable.getX() + radius, _zoomPercentage);
        double zoomed_sin_angle = ZoomController.getZoomedValue(radius * Math.sin(angle), _zoomPercentage);
        double x  = zoomed_centre_x + zoomed_sin_angle;

        double zoomed_centre_y = ZoomController.getZoomedValue(connectable.getY() + radius, _zoomPercentage);
        double zoomed_cos_angle = ZoomController.getZoomedValue(radius * Math.cos(angle), _zoomPercentage);
        double y = zoomed_centre_y +  zoomed_cos_angle;
        Point2D.Double  coord = new Point2D.Double(x, y);
        return coord;
    }

    private Point2D.Double  getTransitionCoordinatesDest(Connectable connectable, double angle)
    {
        double rootThreeOverTwo = 0.5 * Math.sqrt(3);
        AffineTransform transform = AffineTransform.getRotateInstance(0);
        Point2D.Double transformed = new Point2D.Double();

        if (Math.cos(angle) > rootThreeOverTwo) //Top
        {
            transform.transform(new Point2D.Double(1, 0.5 * connectable.getHeight()), transformed);
        }
        else if (Math.cos(angle) < -rootThreeOverTwo)  //Bottom
        {
            transform.transform(new Point2D.Double(0, -0.5 * connectable.getHeight()), transformed);
        }
        else if (Math.sin(angle) > 0) //Left
        {
            transform.transform(new Point2D.Double(-0.5 * connectable.getWidth(), 1), transformed);
            transformed.x += connectable.getWidth();
        } else     //Right
        {
            transform.transform(new Point2D.Double(0.5 * connectable.getWidth(), 0), transformed);
            transformed.x -= connectable.getWidth();
        }

        //double half_width = connectable.getWidth()/2;
        double half_height = connectable.getHeight()/2;
        double x  = ZoomController.getZoomedValue(connectable.getX() + half_height + transformed.x, _zoomPercentage);
        double y = ZoomController.getZoomedValue(connectable.getY() + half_height +  transformed.y, _zoomPercentage);

        Point2D.Double  coord = new Point2D.Double (x, y);
        return coord;
    }

    private Point2D.Double  getTransitionCoordinatesSource(Connectable connectable, double angle)
    {
        double rootThreeOverTwo = 0.5 * Math.sqrt(3);
        AffineTransform transform = AffineTransform.getRotateInstance(0);
        Point2D.Double transformed = new Point2D.Double();

        if (Math.cos(angle) > rootThreeOverTwo)
        {
            transform.transform(new Point2D.Double(1, 0.5 * connectable.getHeight()), transformed);
        }
        else if (Math.cos(angle) < -rootThreeOverTwo)
        {
            transform.transform(new Point2D.Double(0, -0.5 * connectable.getHeight()), transformed);
        }
        else if (Math.sin(angle) > 0)
        {
            transform.transform(new Point2D.Double(-0.5 * connectable.getWidth(), 1), transformed);
        } else
        {
            transform.transform(new Point2D.Double(0.5 * connectable.getWidth(), 0), transformed);
        }

        double half_height = connectable.getHeight()/2;
        double x  = ZoomController.getZoomedValue(connectable.getX() + half_height + transformed.x, _zoomPercentage);
        double y = ZoomController.getZoomedValue(connectable.getY() + half_height +  transformed.y, _zoomPercentage);

        Point2D.Double  coord = new Point2D.Double (x, y);
        return coord;
    }

    ArcView(ConnectableView newSource)
    {
        _source = newSource;
        myPath.addPoint();
        myPath.addPoint();
        myPath.createPath();
    }

    ArcView()
    {
        super();
    }

    void setSource(ConnectableView sourceInput)
    {
        _source = sourceInput;
    }

    public void setTarget(ConnectableView targetInput)
    {
        _target = targetInput;
    }

    public HistoryItem setWeight(LinkedList<MarkingView> weightInput)
    {
        LinkedList<MarkingView> oldWeight = Copier.mediumCopy(_weight);
        checkIfFunctionalWeightExists();
        return new ArcWeight(this, oldWeight, _weight);
    }

    public void setWeightLabelPosition()
    {
        int originalX = (int) (myPath.midPoint.x);
        int originalY = (int) (myPath.midPoint.y) - 10;
        int x = originalX;
        int y = originalY;
        int yCount = 0;
        updateArcLabel();
        for(NameLabel label : weightLabel)
        {
            if(yCount >= 4)
            {
                y = originalY;
                x += 17;
                yCount = 0;
            }
            // we must trim here as the removeLabelsFromArc adds blank spaces
            if(!label.getText().trim().equals(""))// && Integer.valueOf(label.getText()) > 0)
            {
                label.setPosition(x + label.getWidth() / 2 - 4, y);
                y += 10;
                yCount++;
            }
        }
    }

	private void updateArcLabel() {
		for(int i=0;i<weightLabel.size();i++){
			try{
				Integer.valueOf(_weight.get(i).getCurrentFunctionalMarking());
				weightLabel.get(i).setText(_weight.get(i).getCurrentMarking()+"");
			}catch(Exception e){
				if(_weight.get(i).getCurrentMarking()==0){
					weightLabel.get(i).setText(_weight.get(i).getCurrentFunctionalMarking());
				}else
					weightLabel.get(i).setText(_weight.get(i).getCurrentFunctionalMarking()+"="+_weight.get(i).getCurrentMarking());
			}
		}
	}

	// Removes any current labels from the arc
    void removeLabelsFromArc()
    {
        int x = (int) (myPath.midPoint.x);
        int y = (int) (myPath.midPoint.y) - 10;
        for(NameLabel label : weightLabel)
        {
            if(!label.getText().trim().equals("") )//&& Integer.valueOf(label.getText()) > 0)
            {
                // Put blank spaces over previous label
                //	NameLabel newLabel = new NameLabel(zoom);
                int currentLength = label.getText().length();
                label.setText("");
                for(int i = 0; i < currentLength; i++)
                {
                    label.setText(label.getText() + " ");
                }
                label.setPosition(x + label.getWidth() / 2 - 4, y);
                y += 10;
            }
            removeLabelFromParentContainer(label);
        }
        weightLabel.clear();
    }

	protected void removeLabelFromParentContainer(NameLabel label)
	{
		getParent().remove(label);
	}

    public String getId()
    {
        if(_id != null)
        {
            return _id;
        }
        else
        {
            if(_source != null && _target != null)
            {
                return _source.getId() + " to " + _target.getId();
            }
        }
        return "";
    }

    public String getName()
    {
        return getId();
    }

    public ConnectableView getSource()
    {
        return _source;
    }

    public ConnectableView getTarget()
    {
        return _target;
    }

    public ConnectableView getTheOtherEndFor(ConnectableView connectableView)
    {
        if(_source == connectableView)
            return _target;
        return _source;
    }

    public double getStartPositionX()
    {
        return myPath.getPoint(0).getX();
    }

    public double getStartPositionY()
    {
        return myPath.getPoint(0).getY();
    }

    public int getSimpleWeight()
    {
        return 1;
    }

    public void updateArcPosition()
    {
        Pair<Point2D.Double, Point2D.Double> points = getArcStartAndEnd();
        setSourceLocation(points.first.x, points.first.y);
        setTargetLocation(points.second.x, points.second.y);
        myPath.createPath();
    }

    public void setEndPoint(double x, double y, boolean type)
    {
        myPath.setPointLocation(myPath.getEndIndex(), x, y);
        myPath.setPointType(myPath.getEndIndex(), type);
        updateArcPosition();
    }

    public void setTargetLocation(double x, double y)
    {
        myPath.setPointLocation(myPath.getEndIndex(), x, y);
        myPath.createPath();
        updateBounds();
        repaint();
    }

    public void setSourceLocation(double x, double y)
    {
        myPath.setPointLocation(0, x, y);
        myPath.createPath();
        updateBounds();
        repaint();
    }

    /**
     * Updates the bounding box of the arc component based on the arcs bounds
     */
    void updateBounds()
    {
        _bounds = myPath.getBounds();
        _bounds.grow(getComponentDrawOffset() + zoomGrow, getComponentDrawOffset()
                + zoomGrow);
        setBounds(_bounds);
    }

    public ArcPath getArcPath()
    {
        return myPath;
    }

    public boolean contains(int x, int y)
    {
        Point2D.Double point = new Point2D.Double(x + myPath.getBounds().getX()
                                                          - getComponentDrawOffset() - zoomGrow, y
                + myPath.getBounds().getY() - getComponentDrawOffset() - zoomGrow);
        if(!ApplicationSettings.getApplicationView().getCurrentTab().isInAnimationMode())
        {
            if(myPath.proximityContains(point) || _selected)
            {
                // show also if Arc itself selected
                myPath.showPoints();
            }
            else
            {
                myPath.hidePoints();
            }
        }

        return myPath.contains(point);
    }

    public void addedToGui()
    {
        // called by PetriNetTab / State viewer when adding component.
        _deleted = false;
        _markedAsDeleted = false;

        if(getParent() instanceof PetriNetTab)
        {
            myPath.addPointsToGui((PetriNetTab) getParent());
        }
        else
        {
            myPath.addPointsToGui((JLayeredPane) getParent());
        }
        updateArcPosition();
        Container parent = getParent();
        if(parent != null)
        {
            int i = 0;
            for(NameLabel label : weightLabel)
            {
                if(label.getParent() == null)
                {
                    i++;
                    parent.add(label);
                }
            }
        }
    }

    public void delete()
    {
        if(!_deleted)
        {
            for(NameLabel label : weightLabel)
            {
                removeLabelFromParentContainer(label);
            }
            myPath.forceHidePoints();
            super.delete();
            _deleted = true;
            ApplicationSettings.getApplicationView().getCurrentPetriNetView().deleteArc(this.getId());
        }
    }

    public void setPathToTransitionAngle(int angle)
    {
        myPath.set_transitionAngle(angle);
    }

    public HistoryItem split(Point2D.Float mouseposition)
    {
        ArcPathPoint newPoint = myPath.splitSegment(mouseposition);
        return new AddArcPathPoint(this, newPoint);
    }

    public abstract String getType();

    public boolean inView()
    {
        return inView;
    }

    public TransitionView getTransition()
    {
        if(getTarget() instanceof TransitionView)
        {
            return (TransitionView) getTarget();
        }
        else
        {
            return (TransitionView) getSource();
        }
    }

    public void removeFromView()
    {
        if(getParent() != null)
        {
            for(NameLabel label : weightLabel)
            {
                removeLabelFromParentContainer(label);
            }
        }
        myPath.forceHidePoints();
        removeFromContainer();
    }

    public void addToView(PetriNetTab view)
    {
        if(getParent() != null)
        {
            for(NameLabel label : weightLabel)
            {
                getParent().add(label);
            }
        }
        myPath.showPoints();
        view.add(this);
    }

    public int getLayerOffset()
    {
        return Constants.ARC_LAYER_OFFSET;
    }

    public void translate(int x, int y)
    {
        // We don't translate an arc, we translate each selected arc point
    }

    public void zoomUpdate(int percent)
    {
        _zoomPercentage = percent;
        this.updateArcPosition();
        for(NameLabel label : weightLabel)
        {
            label.zoomUpdate(percent);
            label.updateSize();
        }
    }

    public void setZoom(int percent)
    {
        _zoomPercentage = percent;
    }

    public void undelete(PetriNetView model, PetriNetTab view)
    {
        if(this.isDeleted())
        {
            model.addPetriNetObject(this);
            view.add(this);
            getSource().addOutbound(this);
            getTarget().addInbound(this);
        }
    }

    /**
     * Method to clone an Arc object
     */
    public PetriNetViewComponent clone()
    {
        return super.clone();
    }

    public void showEditor()
    {
        // Build interface
        EscapableDialog guiDialog = new EscapableDialog(ApplicationSettings.getApplicationView(),
                                                        "PIPE2", true);

        ArcWeightEditorPanel arcWeightEditor = new ArcWeightEditorPanel(
                guiDialog.getRootPane(), this, ApplicationSettings.getApplicationView().getCurrentPetriNetView(), ApplicationSettings.getApplicationView().getCurrentTab());

        guiDialog.add(arcWeightEditor);

        guiDialog.getRootPane().setDefaultButton(null);

        guiDialog.setResizable(false);

        // Make window fit contents' preferred size
        guiDialog.pack();

        // Move window to the middle of the screen
        guiDialog.setLocationRelativeTo(null);

        guiDialog.setVisible(true);

        guiDialog.dispose();
    }

    // Accessor function to check whether or not the Arc is tagged
    public boolean isTagged()
    {
        return false;
    }

    public LinkedList<MarkingView> getWeightSimple(){
    	return _weight;
    }
    
    public boolean isWeightFunctional(){
    	return !_noFunctionalWeights;
    }
    
    public LinkedList<MarkingView> getConstantWeight(){
    	if(_noFunctionalWeights){
    		return _weight;
    	}
    	for(int i=0;i<_weight.size();i++){
			_weight.get(i).setCurrentMarking(_weight.get(i).getCurrentMarking()+"");
    	}
    	return _weight;
    }
    
    public LinkedList<MarkingView> getWeight()
    {
		return _weight;
    }
    
    public int getWeightOfTokenClass(String id)
    {
        if(_weight != null)
        {
            for(MarkingView m : _weight)
            {
                if(m.getToken().getID().equals(id))
                {
                	if(m.getCurrentMarking() == -1){
    					JOptionPane.showMessageDialog(null,
    							"Error in weight expression. Please make sure\r\n it is an integer");
    				}
                	if(m.getCurrentMarking()==0){
                		return 1;
                	}
                        return m.getCurrentMarking();
                }
            }
        }
        
        return 0;
    }
    public String getWeightFunctionOfTokenClass(String id)
    {
        if(_weight != null)
        {
            for(MarkingView m : _weight)
            {
                if(m.getToken().getID().equals(id))
                {
                        return m.getCurrentFunctionalMarking();
                }
            }
        }
        return "";
    }

    public void setWeightFunctionByID(String id, String func) {
		if(_weight != null)
        {
            for(MarkingView m : _weight)
            {
                if(m.getToken().getID().equals(id))
                {
                        m.setCurrentMarking(func);
                }
            }
        }
		checkIfFunctionalWeightExists();
	}

	public void checkIfFunctionalWeightExists() {
		if(_weight != null)
        {
            for(MarkingView m : _weight)
            {
            	try{
            		Integer.parseInt(m.getCurrentFunctionalMarking());
            		_noFunctionalWeights=true;
            	}catch (Exception e){
            		_noFunctionalWeights=false;
            		return;
            	}
            	
            }
        }
	}
	//TODO determine which lists really need to be updated, and remove the argument.  
	public void addThisAsObserverToWeight(List<MarkingView> weights) 
	{ 
		for (MarkingView markingView : weights)
		{
			markingView.addObserver(this);
		}
	}
	// Steve Doubleday (Oct 2013): cascading clean up of Marking Views if Token View is disabled
	@Override
	public void update(Observable observable, Object obj)
	{
		if ((observable instanceof PipeObservable) && (obj == null))
		{
			// if multiple cases are added, consider creating specific subclasses of Observable
			Object originalObject = ((PipeObservable) observable).getObservable();
			if (originalObject instanceof MarkingView)
			{
				MarkingView viewToDelete = (MarkingView) originalObject;
				_weight.remove(viewToDelete);
				HistoryItem historyItem = this.setWeight(_weight);
				updateHistory(historyItem);
			}
		}
	}

	protected void updateHistory(HistoryItem historyItem)
	{ // Steve Doubleday:  changed from addEdit to avoid NPE when HistoryManager edits is list of nulls 
		ApplicationSettings.getApplicationView().getCurrentTab().getHistoryManager().addNewEdit(historyItem);  
	}

    public Arc getModel() {
        return _model;
    }
}
