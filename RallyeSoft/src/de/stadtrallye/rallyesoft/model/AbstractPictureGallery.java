package de.stadtrallye.rallyesoft.model;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

abstract class AbstractPictureGallery implements IPictureGallery {
	
	protected int initialPos = 0;
	protected List<Integer> pictures;

	@Override
	public void add(int location, Integer object) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean add(Integer object) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(int location, Collection<? extends Integer> collection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends Integer> collection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(Object object) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(Collection<?> collection) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Integer get(int location) {
		return pictures.get(location);
	}

	@Override
	public int indexOf(Object object) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<Integer> iterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int lastIndexOf(Object object) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<Integer> listIterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<Integer> listIterator(int location) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Integer remove(int location) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object object) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> collection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> collection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Integer set(int location, Integer object) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return pictures.size();
	}

	@Override
	public List<Integer> subList(int start, int end) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T[] toArray(T[] array) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getInitialPosition() {
		return initialPos;
	}
}
