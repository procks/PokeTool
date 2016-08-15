package com.yuralex.poketool;

/**
 * Created by heiderlopes on 11/12/14.
 */
public class PokemonImg {
    private int id;
    private String nome;
    private String tipo;
    private int imagem;

    public PokemonImg() {

    }

    public PokemonImg(int id, String nome, String tipo, int imagem){
        this.id = id;
        this.nome = nome;
        this.tipo = tipo;
        this.imagem = imagem;

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getImagem() {
        return imagem;
    }

    public void setImagem(int imagem) {
        this.imagem = imagem;
    }
}